<?php
declare(strict_types=1);

require_once __DIR__ . '/../../config/push.php';
require_once __DIR__ . '/../../config/auth.php';

api_bootstrap();
require_method('POST');
require_admin_api();
verify_csrf();

$title = clean_string($_POST['title'] ?? '', PUSH_TITLE_MAX);
$body = clean_string($_POST['body'] ?? '', PUSH_BODY_MAX);
$link = clean_string($_POST['link'] ?? '', 500);
$target = strtolower(clean_string($_POST['target'] ?? 'all', 20));
$image = $_FILES['image'] ?? null;
$hasImage = is_array($image) && ($image['error'] ?? UPLOAD_ERR_NO_FILE) !== UPLOAD_ERR_NO_FILE;

$errors = [];
if ($title === '') {
    $errors['title'] = 'Title is required';
}
if ($body === '') {
    $errors['body'] = 'Description is required';
}
if ($link !== '' && !is_valid_url($link)) {
    $errors['link'] = 'Enter a valid URL (https://...)';
}
if (!in_array($target, PUSH_TARGETS, true)) {
    $errors['target'] = 'Choose who receives the notification';
}
$serviceAccount = get_service_account();
if ($serviceAccount === null) {
    $errors['service_account'] = 'Upload the Firebase service account JSON first';
}
if ($errors) {
    validation_error($errors);
}

$tokens = push_tokens($target);
if (!$tokens) {
    json_response(false, 'No users can receive notifications yet', null, 422);
}

$imageUrl = '';
if ($hasImage) {
    try {
        $imageUrl = store_push_image($image);
    } catch (InvalidArgumentException $e) {
        validation_error(['image' => $e->getMessage()]);
    }
}

$pdo = db();
$pdo->prepare(
    'INSERT INTO push_notifications (title, body, image_url, link, target, total_count) VALUES (:title, :body, :image, :link, :target, :total)'
)->execute([
    ':title' => $title,
    ':body' => $body,
    ':image' => $imageUrl,
    ':link' => $link,
    ':target' => $target,
    ':total' => count($tokens),
]);
$id = (int) $pdo->lastInsertId();

set_time_limit(0);
try {
    [$success, $failure] = fcm_send($serviceAccount, $tokens, [
        'id' => (string) $id,
        'title' => $title,
        'body' => $body,
        'image' => $imageUrl,
        'link' => $link,
    ]);
} catch (RuntimeException $e) {
    $pdo->prepare('UPDATE push_notifications SET failure_count = total_count WHERE id = :id')->execute([':id' => $id]);
    json_response(false, $e->getMessage(), null, 502);
}

$pdo->prepare('UPDATE push_notifications SET success_count = :s, failure_count = :f WHERE id = :id')
    ->execute([':s' => $success, ':f' => $failure, ':id' => $id]);

json_response(true, "Sent to $success of " . count($tokens) . ' users', [
    'total' => count($tokens),
    'success' => $success,
    'failure' => $failure,
]);

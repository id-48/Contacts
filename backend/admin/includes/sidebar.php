<?php
/** @var string $pageKey */
declare(strict_types=1);

$menu = [
    ['key' => 'dashboard', 'label' => 'Dashboard', 'href' => 'index.php',
        'icon' => '<path d="M3 13h8V3H3v10zm0 8h8v-6H3v6zm10 0h8V11h-8v10zm0-18v6h8V3h-8z"/>'],
    ['key' => 'settings', 'label' => 'App Settings', 'href' => 'app-settings.php',
        'icon' => '<path d="M19.4 13a7.5 7.5 0 0 0 0-2l2.1-1.6-2-3.5-2.5 1a7.3 7.3 0 0 0-1.7-1L15 3h-4l-.4 2.9a7.3 7.3 0 0 0-1.7 1l-2.5-1-2 3.5L6.6 11a7.5 7.5 0 0 0 0 2l-2.1 1.6 2 3.5 2.5-1c.5.4 1.1.7 1.7 1L11 21h4l.4-2.9c.6-.3 1.2-.6 1.7-1l2.5 1 2-3.5L19.4 13zM13 15.5a3.5 3.5 0 1 1 0-7 3.5 3.5 0 0 1 0 7z"/>'],
    ['key' => 'ads', 'label' => 'Ads Configuration', 'href' => 'ads-config.php',
        'icon' => '<path d="M4 4h16a1 1 0 0 1 1 1v14a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V5a1 1 0 0 1 1-1zm2.5 12h2l.5-1.5h2.5L12 16h2L11.2 8H9.3L6.5 16zm3.1-3.2.8-2.5.8 2.5H9.6zM15 8v8h2.2a3 3 0 0 0 3-3v-2a3 3 0 0 0-3-3H15zm1.8 1.7h.4c.7 0 1.2.5 1.2 1.2v2.2c0 .7-.5 1.2-1.2 1.2h-.4V9.7z"/>'],
    ['key' => 'users', 'label' => 'Users', 'href' => 'users.php',
        'icon' => '<path d="M16 11a4 4 0 1 0-3.9-5A4 4 0 0 0 16 11zM8 11a3 3 0 1 0 0-6 3 3 0 0 0 0 6zm0 2c-2.7 0-8 1.3-8 4v2h7v-2.2c0-1.4.8-2.6 2-3.5A12 12 0 0 0 8 13zm8 0c-2.7 0-8 1.3-8 4v2h16v-2c0-2.7-5.3-4-8-4z"/>'],
    ['key' => 'notifications', 'label' => 'Send Notification', 'href' => 'notifications.php',
        'icon' => '<path d="M12 22a2 2 0 0 0 2-2h-4a2 2 0 0 0 2 2zm6-6v-5c0-3.1-1.6-5.6-4.5-6.3V4a1.5 1.5 0 0 0-3 0v.7C7.6 5.4 6 7.9 6 11v5l-2 2v1h16v-1l-2-2z"/>'],
    ['key' => 'facebook', 'label' => 'Facebook Marketing', 'href' => 'facebook.php',
        'icon' => '<path d="M22 12a10 10 0 1 0-11.6 9.9v-7H7.9V12h2.5V9.8c0-2.5 1.5-3.9 3.8-3.9 1.1 0 2.2.2 2.2.2v2.5h-1.2c-1.2 0-1.6.8-1.6 1.6V12h2.8l-.4 2.9h-2.3v7A10 10 0 0 0 22 12z"/>'],
];
?>
<aside class="sidebar" id="sidebar">
    <div class="brand">
        <span class="brand-logo">
            <svg viewBox="0 0 24 24" width="20" height="20"><path fill="currentColor" d="M6.6 10.8a15 15 0 0 0 6.6 6.6l2.2-2.2c.3-.3.7-.4 1-.2 1.1.4 2.3.6 3.6.6.6 0 1 .4 1 1V20c0 .6-.4 1-1 1A17 17 0 0 1 3 4c0-.6.4-1 1-1h3.5c.6 0 1 .4 1 1 0 1.3.2 2.5.6 3.6.1.3 0 .7-.2 1l-2.3 2.2z"/></svg>
        </span>
        <span class="brand-name"><?= e(APP_NAME) ?></span>
    </div>
    <nav class="nav">
        <?php foreach ($menu as $item): ?>
            <a class="nav-link<?= $pageKey === $item['key'] ? ' active' : '' ?>" href="<?= e($item['href']) ?>">
                <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><?= $item['icon'] ?></svg>
                <span><?= e($item['label']) ?></span>
            </a>
        <?php endforeach; ?>
        <a class="nav-link nav-logout" href="logout.php?token=<?= e(csrf_token()) ?>">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M10 17l1.4 1.4L17.8 12l-6.4-6.4L10 7l4 4H3v2h11l-4 4zm9-14H12v2h7v14h-7v2h7a2 2 0 0 0 2-2V5a2 2 0 0 0-2-2z"/></svg>
            <span>Logout</span>
        </a>
    </nav>
</aside>

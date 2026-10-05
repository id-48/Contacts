<?php
declare(strict_types=1);

require_once __DIR__ . '/../../config/push.php';
require_once __DIR__ . '/../../config/auth.php';

api_bootstrap();
require_method('GET');
require_admin_api();

json_response(true, 'Notifications loaded', [
    'reachable' => push_reachable_counts(),
    'history' => push_history(),
]);

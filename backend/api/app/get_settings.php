<?php
declare(strict_types=1);

require_once __DIR__ . '/../../config/repository.php';
require_once __DIR__ . '/../../config/auth.php';

api_bootstrap();
require_method('GET');
require_admin_api();

json_response(true, 'Settings loaded', get_admin_settings());

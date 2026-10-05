<?php
declare(strict_types=1);

// Public endpoint used by the Android app. Returns only public configuration.
require_once __DIR__ . '/../../config/repository.php';

api_bootstrap();
require_method('GET');

json_response(true, 'Configuration loaded successfully', build_public_config());

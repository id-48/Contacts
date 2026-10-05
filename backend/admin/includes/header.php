<?php
/** @var string $pageTitle */
/** @var string $pageKey */
declare(strict_types=1);
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="csrf-token" content="<?= e(csrf_token()) ?>">
    <title><?= e($pageTitle) ?> · <?= e(APP_NAME) ?></title>
    <link rel="stylesheet" href="<?= e(asset('css/admin.css')) ?>">
</head>
<body data-page="<?= e($pageKey) ?>">
<div class="layout">
    <?php require __DIR__ . '/sidebar.php'; ?>
    <div class="sidebar-backdrop" data-sidebar-close></div>
    <div class="main">
        <header class="topbar">
            <button class="icon-btn menu-btn" type="button" aria-label="Open menu" data-sidebar-open>
                <svg viewBox="0 0 24 24" width="22" height="22"><path d="M3 6h18M3 12h18M3 18h18" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>
            </button>
            <h1 class="topbar-title"><?= e($pageTitle) ?></h1>
            <div class="topbar-user">
                <span class="avatar"><?= e(strtoupper(substr(current_admin_email(), 0, 1))) ?></span>
                <span class="topbar-email"><?= e(current_admin_email()) ?></span>
            </div>
        </header>
        <main class="content">

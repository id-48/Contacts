<?php
declare(strict_types=1);

$appName = 'Contacts';
$contactEmail = 'tuantzu161@gmail.com';
$effectiveDate = 'October 5, 2026';

header('Content-Type: text/html; charset=utf-8');
header('X-Content-Type-Options: nosniff');
header('Referrer-Policy: no-referrer');

function e(string $value): string
{
    return htmlspecialchars($value, ENT_QUOTES, 'UTF-8');
}
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="color-scheme" content="light dark">
    <title>Privacy Policy – <?= e($appName) ?></title>
    <style>
        :root {
            --bg: #ffffff;
            --card: #f4f5f7;
            --text: #16181d;
            --muted: #6b7080;
            --primary: #3d7ef7;
            --border: #e6e8ec;
        }

        @media (prefers-color-scheme: dark) {
            :root {
                --bg: #000000;
                --card: #1c1d21;
                --text: #f2f3f5;
                --muted: #9a9eaa;
                --primary: #4c8bff;
                --border: #2a2c31;
            }
        }

        * {
            box-sizing: border-box;
        }

        body {
            margin: 0;
            background: var(--bg);
            color: var(--text);
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
            font-size: 16px;
            line-height: 1.6;
        }

        main {
            max-width: 760px;
            margin: 0 auto;
            padding: 32px 20px 56px;
        }

        header {
            padding-bottom: 20px;
            border-bottom: 1px solid var(--border);
        }

        h1 {
            margin: 0 0 6px;
            font-size: 28px;
            line-height: 1.25;
        }

        h2 {
            margin: 32px 0 10px;
            font-size: 20px;
            line-height: 1.3;
        }

        h3 {
            margin: 18px 0 6px;
            font-size: 16px;
        }

        p, ul {
            margin: 0 0 12px;
        }

        ul {
            padding-left: 22px;
        }

        li {
            margin-bottom: 6px;
        }

        a {
            color: var(--primary);
        }

        .meta {
            color: var(--muted);
            font-size: 14px;
        }

        .note {
            margin: 20px 0 0;
            padding: 14px 16px;
            background: var(--card);
            border-radius: 12px;
        }

        footer {
            margin-top: 40px;
            padding-top: 20px;
            border-top: 1px solid var(--border);
            color: var(--muted);
            font-size: 14px;
        }
    </style>
</head>
<body>
<main>
    <header>
        <h1>Privacy Policy</h1>
        <p class="meta"><?= e($appName) ?> &middot; Effective date: <?= e($effectiveDate) ?></p>
    </header>

    <p class="note">
        Your contacts, call history and phone calls stay on your device. We never upload, sell or share them.
    </p>

    <h2>1. Introduction</h2>
    <p>
        This Privacy Policy explains how the <?= e($appName) ?> Android app (the "App", "we", "us") handles
        information when you use it as your contacts manager, phone dialer and, if enabled, home screen launcher.
        By using the App you agree to this policy.
    </p>

    <h2>2. Information processed only on your device</h2>
    <p>
        The App asks for the Android permissions below so its features work. The data they give access to is processed
        on your device and is not sent to our servers or to any third party.
    </p>
    <ul>
        <li><strong>Contacts</strong> – to show, search, add, edit, favorite and delete your contacts.</li>
        <li><strong>Call log</strong> – to show your recent, missed and incoming calls and to delete call history you choose.</li>
        <li><strong>Phone and calls</strong> – to place, answer and manage calls when the App is your default phone app,
            and to show the caller's name from your contacts.</li>
        <li><strong>Call screening and blocked numbers</strong> – to block numbers you add. Blocked numbers are stored in
            Android's system block list.</li>
        <li><strong>Camera and photos</strong> – only when you choose to take or pick a picture for a contact.</li>
        <li><strong>Notifications</strong> – to show incoming and missed calls, reminders and app updates.</li>
        <li><strong>Display over other apps</strong> – to show call screens and the after-call screen.</li>
        <li><strong>Installed apps (launcher mode)</strong> – when the App is set as your home screen, it reads the list of
            apps installed on your phone only to display and open them. You can also uninstall an app from the home
            screen; this happens only when you ask for it and Android confirms it with you.</li>
    </ul>
    <p>
        Settings you choose in the App (theme, language, quick responses and similar) are stored only on your device and are
        removed when you uninstall the App.
    </p>

    <h2>3. Information we collect</h2>
    <p>To operate the App, the App sends a small amount of technical information to our server:</p>
    <ul>
        <li><strong>Device identifier</strong> – Android's app-scoped device ID, so one device is counted as one user,
            even after reinstalling.</li>
        <li><strong>Install source</strong> – whether the App was installed organically or from an ad campaign, read
            from the Google Play Install Referrer.</li>
        <li><strong>App version</strong> and the dates of first install and last use.</li>
        <li><strong>Push notification token</strong> – a Firebase Cloud Messaging token used to send you notifications.</li>
    </ul>
    <p>
        We do not collect your name, phone number, email address, location, contacts, call history or the content of
        your calls.
    </p>

    <h2>4. Third-party services</h2>
    <p>
        The App uses the services below. They may collect information such as your device's advertising ID, IP address,
        device model, operating system and app usage under their own privacy policies.
    </p>
    <h3>Advertising</h3>
    <ul>
        <li>Google AdMob and Google Ad Manager – <a href="https://policies.google.com/privacy" target="_blank" rel="noopener">Google Privacy Policy</a></li>
        <li>TradPlus and its mediation partners, including Meta Audience Network –
            <a href="https://www.tradplusad.com/privacy" target="_blank" rel="noopener">TradPlus Privacy Policy</a>,
            <a href="https://www.facebook.com/privacy/policy/" target="_blank" rel="noopener">Meta Privacy Policy</a></li>
    </ul>
    <h3>Analytics, stability and notifications</h3>
    <ul>
        <li>Firebase Analytics, Firebase Crashlytics and Firebase Cloud Messaging –
            <a href="https://firebase.google.com/support/privacy" target="_blank" rel="noopener">Firebase Privacy and Security</a></li>
        <li>Meta (Facebook) SDK for app events and install measurement –
            <a href="https://www.facebook.com/privacy/policy/" target="_blank" rel="noopener">Meta Privacy Policy</a></li>
        <li>Google Play Install Referrer –
            <a href="https://policies.google.com/privacy" target="_blank" rel="noopener">Google Privacy Policy</a></li>
    </ul>
    <p>Your contacts, call history and calls are never shared with these services.</p>

    <h2>5. How we use information</h2>
    <ul>
        <li>To provide and improve the App's features.</li>
        <li>To show ads, which keep the App free.</li>
        <li>To count users, understand which campaigns bring new users, and fix crashes.</li>
        <li>To send notifications about the App.</li>
        <li>To prompt you to update when a required new version is available.</li>
    </ul>

    <h2>6. Sharing and selling</h2>
    <p>
        We do not sell your personal information. We share information only with the service providers listed above, as
        required to run the App, or when required by law.
    </p>

    <h2>7. Data retention</h2>
    <p>
        Technical information on our server is kept while the App is in use and is deleted on request. Data processed only on
        your device is removed when you uninstall the App or clear its data.
    </p>

    <h2>8. Security</h2>
    <p>
        Information is sent to our server over encrypted HTTPS connections, and access to our server is restricted. No method
        of transmission or storage is completely secure, but we take reasonable measures to protect your information.
    </p>

    <h2>9. Your choices</h2>
    <ul>
        <li>You can allow or revoke any permission at any time in your phone's Settings.</li>
        <li>You can reset your advertising ID or opt out of personalized ads in your phone's Google settings.</li>
        <li>You can turn off notifications in your phone's Settings.</li>
        <li>You can stop using launcher mode by choosing another home app in your phone's Settings.</li>
        <li>You can stop all data collection by uninstalling the App.</li>
        <li>To delete the information stored on our server, email us at the address below.</li>
    </ul>

    <h2>10. Children's privacy</h2>
    <p>
        The App is not directed at children under 13, and we do not knowingly collect personal information from children.
        If you believe a child has provided us information, contact us and we will delete it.
    </p>

    <h2>11. Changes to this policy</h2>
    <p>
        We may update this Privacy Policy from time to time. The updated version will be posted on this page with a new
        effective date.
    </p>

    <h2>12. Contact us</h2>
    <p>
        If you have any questions about this Privacy Policy or want your data deleted, email us at
        <a href="mailto:<?= e($contactEmail) ?>"><?= e($contactEmail) ?></a>.
    </p>

    <footer>
        &copy; <?= e(date('Y')) ?> <?= e($appName) ?>. All rights reserved.
    </footer>
</main>
</body>
</html>

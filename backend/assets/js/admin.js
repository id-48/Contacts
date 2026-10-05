(function () {
    'use strict';

    const API = '../api/';
    const VERSION_RE = /^\d+(\.\d+){0,3}$/;
    const NO_SPACE_RE = /^\S*$/;
    const FULLSCREEN_TYPES = [
        ['interstitial', 'Interstitial'],
        ['reward', 'Reward'],
        ['app_open', 'App Open'],
        ['custom', 'Custom'],
    ];
    const DEFAULT_SEQUENCE = ['interstitial', 'reward', 'app_open', 'custom'];
    const ADX_TYPES = ['banner', 'interstitial', 'native', 'app_open', 'reward'];
    const AD_TYPE_LABELS = {
        banner: 'Banner',
        interstitial: 'Interstitial',
        native: 'Native',
        reward: 'Reward',
        app_open: 'App Open',
    };
    const PRIORITY_TYPES = ['banner', 'interstitial', 'native', 'reward', 'app_open'];
    const PRIORITY_MAX = 4;
    const PRIORITY_SEQUENCE_MAX = 25;

    // ---------- Helpers ----------

    class ApiError extends Error {
        constructor(message, data, status) {
            super(message);
            this.data = data;
            this.status = status;
        }
    }

    const $ = (selector, root = document) => root.querySelector(selector);
    const $$ = (selector, root = document) => Array.from(root.querySelectorAll(selector));

    function esc(value) {
        return String(value ?? '').replace(/[&<>"']/g, (c) => ({
            '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;',
        })[c]);
    }

    function csrfToken() {
        const meta = $('meta[name="csrf-token"]');
        return meta ? meta.content : '';
    }

    async function api(path, { method = 'GET', body } = {}) {
        const options = { method, credentials: 'same-origin', headers: { Accept: 'application/json' } };
        if (body instanceof FormData) {
            options.body = body;
        } else if (body !== undefined) {
            options.headers['Content-Type'] = 'application/json';
            options.body = JSON.stringify(body);
        }
        if (method !== 'GET') {
            options.headers['X-CSRF-Token'] = csrfToken();
        }

        let response;
        try {
            response = await fetch(API + path, options);
        } catch (e) {
            throw new ApiError('Network error. Check your connection and try again.', null, 0);
        }

        let json = null;
        try {
            json = await response.json();
        } catch (e) {
            json = null;
        }

        if (response.status === 401 && document.body.dataset.page !== 'login') {
            window.location.href = 'login.php';
            throw new ApiError('Session expired', null, 401);
        }
        if (!response.ok || !json || !json.success) {
            throw new ApiError((json && json.message) || `Request failed (${response.status})`, json && json.data, response.status);
        }
        return json;
    }

    function toast(message, type = 'success') {
        const stack = $('#toastStack');
        if (!stack) return;
        const el = document.createElement('div');
        el.className = `toast${type === 'error' ? ' toast-error' : ''}`;
        el.setAttribute('role', type === 'error' ? 'alert' : 'status');
        el.textContent = message;
        stack.appendChild(el);
        setTimeout(() => {
            el.classList.add('hide');
            setTimeout(() => el.remove(), 250);
        }, type === 'error' ? 5000 : 3000);
    }

    function setButtonLoading(button, loading) {
        if (!button) return;
        if (loading) {
            button.dataset.label = button.textContent;
            button.disabled = true;
            button.innerHTML = `<span class="spinner"></span>${esc(button.dataset.label.trim())}`;
        } else {
            button.disabled = false;
            button.textContent = button.dataset.label || button.textContent;
        }
    }

    function setSectionLoading(form, loading) {
        form.classList.toggle('is-loading', loading);
    }

    function clearErrors(root) {
        $$('.is-invalid', root).forEach((el) => el.classList.remove('is-invalid'));
        $$('.field-error[data-generated]', root).forEach((el) => el.remove());
        $$('[data-error-for]', root).forEach((el) => { el.textContent = ''; });
    }

    function showErrors(root, errors) {
        const unmatched = [];
        let first = null;
        Object.entries(errors || {}).forEach(([key, message]) => {
            const input = $(`[data-key="${CSS.escape(key)}"]`, root);
            const slot = $(`[data-error-for="${CSS.escape(key)}"]`, root);
            if (input) {
                input.classList.add('is-invalid');
                const p = document.createElement('p');
                p.className = 'field-error';
                p.dataset.generated = '1';
                p.textContent = message;
                (input.closest('.input-group') || input).insertAdjacentElement('afterend', p);
                first = first || input;
            } else if (slot) {
                slot.textContent = message;
                first = first || slot;
            } else {
                unmatched.push(message);
            }
        });
        if (unmatched.length) toast(unmatched.join(' '), 'error');
        if (first) first.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }

    function readForm(form) {
        const data = {};
        $$('[data-key]', form).forEach((el) => {
            data[el.dataset.key] = el.type === 'checkbox' ? el.checked : el.value.trim();
        });
        return data;
    }

    function fillForm(form, data) {
        $$('[data-key]', form).forEach((el) => {
            if (!(el.dataset.key in data)) return;
            const value = data[el.dataset.key];
            if (el.type === 'checkbox') el.checked = Boolean(value);
            else el.value = value ?? '';
        });
    }

    function isValidUrl(value) {
        try {
            const url = new URL(value);
            return ['http:', 'https:', 'market:'].includes(url.protocol);
        } catch (e) {
            return false;
        }
    }

    /** Disable -> loading -> validate -> save -> toast -> reload -> enable. */
    function bindSubmit(form, { validate, save, onErrors }) {
        const reportErrors = (errors) => {
            showErrors(form, errors);
            if (onErrors) onErrors(errors);
        };
        form.addEventListener('submit', async (event) => {
            event.preventDefault();
            const button = $('[type="submit"]', form);
            if (button.disabled) return;
            clearErrors(form);
            setButtonLoading(button, true);
            try {
                const errors = validate ? validate() : {};
                if (Object.keys(errors).length) {
                    reportErrors(errors);
                    return;
                }
                await save();
            } catch (err) {
                if (err.data && err.data.errors) reportErrors(err.data.errors);
                toast(err.message, 'error');
            } finally {
                setButtonLoading(button, false);
            }
        });

        form.addEventListener('input', (event) => {
            const el = event.target;
            if (!el.classList.contains('is-invalid')) return;
            el.classList.remove('is-invalid');
            const anchor = el.closest('.input-group') || el;
            const next = anchor.nextElementSibling;
            if (next && next.dataset.generated) next.remove();
        });
    }

    function formatNumber(n) {
        return Number(n || 0).toLocaleString();
    }

    function formatDate(value) {
        if (!value) return '–';
        const date = new Date(value.replace(' ', 'T') + 'Z');
        return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
    }

    // ---------- Global UI ----------

    function initChrome() {
        $$('[data-sidebar-open]').forEach((btn) => btn.addEventListener('click', () => document.body.classList.add('sidebar-open')));
        $$('[data-sidebar-close]').forEach((el) => el.addEventListener('click', () => document.body.classList.remove('sidebar-open')));
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape') document.body.classList.remove('sidebar-open');
        });
        $$('[data-toggle-password]').forEach((btn) => {
            btn.addEventListener('click', () => {
                const input = document.getElementById(btn.dataset.togglePassword);
                const show = input.type === 'password';
                input.type = show ? 'text' : 'password';
                btn.textContent = show ? 'Hide' : 'Show';
            });
        });
    }

    // ---------- Pages ----------

    const pages = {};

    pages.login = function () {
        const form = $('#loginForm');
        bindSubmit(form, {
            validate() {
                const data = readForm(form);
                const errors = {};
                if (!/^\S+@\S+\.\S+$/.test(data.email)) errors.email = 'Enter a valid email address';
                if (!data.password) errors.password = 'Enter your password';
                return errors;
            },
            async save() {
                const data = readForm(form);
                const json = await api('auth/login.php', { method: 'POST', body: { email: data.email, password: data.password } });
                window.location.href = json.data.redirect;
            },
        });
    };

    pages.dashboard = async function () {
        try {
            const { data } = await api('dashboard/stats.php');
            $$('[data-stat]').forEach((el) => {
                const key = el.dataset.stat;
                el.textContent = key.endsWith('_percent') ? `${data[key]}%` : formatNumber(data[key]);
            });
            const bar = $('#breakdownBar');
            const hasUsers = data.total > 0;
            bar.children[0].style.width = `${hasUsers ? data.organic_percent : 0}%`;
            bar.children[1].style.width = `${hasUsers ? data.marketing_percent : 0}%`;
            renderWeekChart(data.last_7_days);
        } catch (err) {
            toast(err.message, 'error');
            $$('[data-stat]').forEach((el) => { el.textContent = '–'; });
        }
    };

    function renderWeekChart(days) {
        const chart = $('#weekChart');
        const max = Math.max(1, ...days.map((d) => d.organic + d.marketing));
        chart.innerHTML = days.map((d) => {
            const label = new Date(d.date + 'T00:00:00').toLocaleDateString(undefined, { weekday: 'short' });
            const organic = (d.organic / max) * 100;
            const marketing = (d.marketing / max) * 100;
            const title = `${d.date}: ${d.organic} organic, ${d.marketing} marketing`;
            return `<div class="bar-col" title="${esc(title)}">
                <div class="bar-stack">
                    <span class="seg-organic" style="height:${organic}%"></span>
                    <span class="seg-marketing" style="height:${marketing}%"></span>
                </div>
                <span class="bar-label">${esc(label)}</span>
            </div>`;
        }).join('');
    }

    const INTRO_SCREEN_MAX = 4;
    const INTRO_REQUIRED_SCREEN = 3;

    function parseIntroFlow(text) {
        const inner = text.replace(/^\s*\[|\]\s*$/g, '').trim();
        if (!inner) return [];
        const list = [];
        for (const part of inner.split(',').map((p) => p.trim())) {
            const n = Number(part);
            if (!/^\d$/.test(part) || n < 1 || n > INTRO_SCREEN_MAX || list.includes(n)) return null;
            list.push(n);
        }
        return list;
    }

    pages.settings = function () {
        const form = $('#settingsForm');

        async function load() {
            setSectionLoading(form, true);
            try {
                const { data } = await api('app/get_settings.php');
                fillForm(form, data);
            } catch (err) {
                toast(err.message, 'error');
            } finally {
                setSectionLoading(form, false);
            }
        }

        bindSubmit(form, {
            validate() {
                const d = readForm(form);
                const errors = {};
                ['privacy_policy_url', 'app_link'].forEach((key) => {
                    if (d[key] && !isValidUrl(d[key])) errors[key] = 'Enter a valid URL (https://...)';
                });
                if (d.app_version && !VERSION_RE.test(d.app_version)) errors.app_version = 'Use a version like 1.0.0';
                if (d.minimum_version && !VERSION_RE.test(d.minimum_version)) errors.minimum_version = 'Use a version like 1.0.0';
                if (d.force_update) {
                    if (!d.app_version) errors.app_version = 'App version is required when force update is enabled';
                    if (!d.minimum_version) errors.minimum_version = 'Minimum version is required when force update is enabled';
                    if (!d.force_update_message) errors.force_update_message = 'Update message is required when force update is enabled';
                    if (!d.app_link) errors.app_link = 'App link is required when force update is enabled';
                }
                ['intro_flow_organic', 'intro_flow_marketing'].forEach((key) => {
                    const flow = parseIntroFlow(d[key]);
                    if (flow === null) errors[key] = 'Use numbers 1-4 without repeats, e.g. 4,1,2,3';
                    else if (!flow.includes(INTRO_REQUIRED_SCREEN)) errors[key] = 'Default (3) is required, e.g. 4,1,2,3';
                });
                return errors;
            },
            async save() {
                const json = await api('app/save_settings.php', { method: 'POST', body: readForm(form) });
                fillForm(form, json.data);
                toast('Settings saved');
            },
        });

        load();
    };

    pages.ads = function () {
        const form = $('#adsForm');
        const SOURCES = ['organic', 'marketing'];
        const SOURCE_LABELS = { organic: 'Organic', marketing: 'Marketing' };
        const state = { sequence: { organic: [], marketing: [] }, source: 'organic' };
        const panel = (source) => $(`[data-source-panel="${source}"]`, form);
        const sourceOf = (el) => el.closest('[data-source-panel]').dataset.sourcePanel;

        // Ad units
        function renderUnits(provider, units) {
            const container = $(`[data-units="${provider}"]`, form);
            const placeholder = provider === 'admob' ? 'ca-app-pub-xxxxxxxx/xxxxxxxx' : 'TradPlus unit ID';
            container.innerHTML = Object.keys(units).map((type) => `
                <div class="unit-row">
                    <span class="unit-label">${esc(AD_TYPE_LABELS[type] || type)}</span>
                    <label class="switch" aria-label="${esc(AD_TYPE_LABELS[type])} enabled">
                        <input type="checkbox" data-key="${provider}.${type}.enabled">
                        <span class="slider"></span>
                    </label>
                    <input type="text" data-key="${provider}.${type}.id" placeholder="${esc(placeholder)}" maxlength="191" spellcheck="false">
                </div>`).join('');
            Object.entries(units).forEach(([type, unit]) => {
                $(`[data-key="${provider}.${type}.enabled"]`, container).checked = Boolean(unit.enabled);
                $(`[data-key="${provider}.${type}.id"]`, container).value = unit.id || '';
            });
        }

        // ADX weighted IDs
        function renderAdx(source, adx) {
            const container = $('[data-adx-groups]', panel(source));
            container.innerHTML = ADX_TYPES.map((type) => `
                <div class="adx-group" data-adx-type="${type}">
                    <div class="adx-group-head">
                        <h3>ADX ${esc(type)}</h3>
                        <span class="muted" data-adx-total></span>
                    </div>
                    <div data-adx-rows></div>
                    <p class="field-error" data-error-for="${source}.adx.${type}"></p>
                    <button class="btn btn-ghost btn-sm" type="button" data-adx-add>+ Add ID</button>
                </div>`).join('');
            ADX_TYPES.forEach((type) => {
                const group = $(`[data-adx-type="${type}"]`, container);
                ((adx || {})[type] || []).forEach((row) => addAdxRow(group, row.id, row.weight));
                refreshAdxGroup(group);
            });
        }

        function addAdxRow(group, id = '', weight = 50) {
            const row = document.createElement('div');
            row.className = 'adx-row';
            row.innerHTML = `
                <input type="text" data-field="id" placeholder="/1234567/ad_unit" maxlength="191" spellcheck="false">
                <input type="number" data-field="weight" min="0" step="1" placeholder="Weight">
                <button class="btn btn-danger-ghost btn-sm" type="button" data-adx-remove>Remove</button>
                <span class="share"></span>`;
            $('[data-field="id"]', row).value = id;
            $('[data-field="weight"]', row).value = weight;
            $('[data-adx-rows]', group).appendChild(row);
        }

        function refreshAdxGroup(group) {
            const type = group.dataset.adxType;
            const source = sourceOf(group);
            const rows = $$('.adx-row', group);
            const weights = rows.map((row) => Math.max(0, parseInt($('[data-field="weight"]', row).value, 10) || 0));
            const total = weights.reduce((a, b) => a + b, 0);
            rows.forEach((row, index) => {
                $('[data-field="id"]', row).dataset.key = `${source}.adx.${type}.${index}.id`;
                $('[data-field="weight"]', row).dataset.key = `${source}.adx.${type}.${index}.weight`;
                $('.share', row).textContent = total > 0 ? `≈ ${((weights[index] / total) * 100).toFixed(1)}% of requests` : '';
            });
            const existingEmpty = $('.adx-empty', group);
            if (!rows.length && !existingEmpty) {
                $('[data-adx-rows]', group).insertAdjacentHTML('beforebegin', '<p class="adx-empty">No IDs yet.</p>');
            } else if (rows.length && existingEmpty) {
                existingEmpty.remove();
            }
            $('[data-adx-total]', group).textContent = rows.length ? `Total weight: ${total}` : '';
        }

        $$('[data-adx-groups]', form).forEach((container) => {
            container.addEventListener('click', (event) => {
                const group = event.target.closest('.adx-group');
                if (!group) return;
                if (event.target.closest('[data-adx-add]')) {
                    addAdxRow(group, '', 50);
                    refreshAdxGroup(group);
                    $('.adx-row:last-child [data-field="id"]', group).focus();
                } else if (event.target.closest('[data-adx-remove]')) {
                    event.target.closest('.adx-row').remove();
                    clearErrors(group);
                    refreshAdxGroup(group);
                }
            });
            container.addEventListener('input', (event) => {
                if (event.target.dataset.field === 'weight') refreshAdxGroup(event.target.closest('.adx-group'));
            });
        });

        // Network priority
        function renderPriority(source, priority) {
            PRIORITY_TYPES.forEach((type) => {
                const p = (priority || {})[type] || {};
                $(`[data-key="${source}.priority.${type}.main"]`, form).value = (p.main || []).join(',');
                $(`[data-key="${source}.priority.${type}.failed"]`, form).value = (p.failed || []).join(',');
            });
        }

        function parsePriority(text, allowRepeats = false) {
            const inner = text.replace(/^\s*\[|\]\s*$/g, '').trim();
            if (!inner) return [];
            const parts = inner.split(',').map((part) => part.trim());
            const list = [];
            for (const part of parts) {
                const n = Number(part);
                if (!/^\d$/.test(part) || n < 1 || n > PRIORITY_MAX || (!allowRepeats && list.includes(n))) return null;
                list.push(n);
            }
            return allowRepeats && list.length > PRIORITY_SEQUENCE_MAX ? null : list;
        }

        // Fullscreen sequence
        function renderPalette(source) {
            $('[data-sequence-palette]', panel(source)).innerHTML = FULLSCREEN_TYPES.map(([type, label], index) => `
                <button class="chip" type="button" data-sequence-add="${type}">
                    <span class="chip-num">${index + 1}</span>${esc(label)}
                </button>`).join('');
        }

        function renderSequence(source) {
            const list = $('[data-sequence-list]', panel(source));
            const sequence = state.sequence[source];
            if (!sequence.length) {
                list.innerHTML = '<span class="sequence-empty">No fullscreen ads will be shown. Click a type above to add one.</span>';
                return;
            }
            const labels = Object.fromEntries(FULLSCREEN_TYPES);
            list.innerHTML = sequence.map((type, index) => `
                <button class="chip chip-active" type="button" data-sequence-remove="${index}" title="Click to remove">
                    <span class="chip-num">${index + 1}</span>${esc(labels[type] || type)}
                </button>`).join('');
        }

        SOURCES.forEach((source) => {
            const root = panel(source);
            renderPalette(source);
            $('[data-sequence-palette]', root).addEventListener('click', (event) => {
                const chip = event.target.closest('[data-sequence-add]');
                if (!chip) return;
                if (state.sequence[source].length >= 50) {
                    toast('Maximum 50 sequence items', 'error');
                    return;
                }
                state.sequence[source].push(chip.dataset.sequenceAdd);
                renderSequence(source);
            });
            $('[data-sequence-list]', root).addEventListener('click', (event) => {
                const chip = event.target.closest('[data-sequence-remove]');
                if (!chip) return;
                state.sequence[source].splice(Number(chip.dataset.sequenceRemove), 1);
                renderSequence(source);
            });
            $('[data-sequence-reset]', root).addEventListener('click', () => {
                state.sequence[source] = DEFAULT_SEQUENCE.slice();
                renderSequence(source);
            });
        });

        // Organic / Marketing tabs
        const otherSource = (source) => (source === 'organic' ? 'marketing' : 'organic');

        function showSource(source) {
            state.source = source;
            $$('[data-source-tab]', form).forEach((tab) => {
                const active = tab.dataset.sourceTab === source;
                tab.classList.toggle('is-active', active);
                tab.setAttribute('aria-selected', String(active));
            });
            SOURCES.forEach((s) => { panel(s).hidden = s !== source; });
            $('[data-copy-source]', form).textContent = `Copy to ${SOURCE_LABELS[otherSource(source)]}`;
        }

        function markSourceErrors(errors) {
            const keys = Object.keys(errors || {});
            const withErrors = SOURCES.filter((source) => keys.some((key) => key.startsWith(`${source}.`)));
            $$('[data-source-tab]', form).forEach((tab) => {
                tab.classList.toggle('has-error', withErrors.includes(tab.dataset.sourceTab));
            });
            const sharedErrors = keys.some((key) => !SOURCES.some((source) => key.startsWith(`${source}.`)));
            if (withErrors.length && !withErrors.includes(state.source) && !sharedErrors) {
                showSource(withErrors[0]);
                const first = $('.is-invalid, .field-error:not(:empty)', panel(withErrors[0]));
                if (first) first.scrollIntoView({ behavior: 'smooth', block: 'center' });
            }
        }

        $$('[data-source-tab]', form).forEach((tab) => {
            tab.addEventListener('click', () => showSource(tab.dataset.sourceTab));
        });

        $('[data-copy-source]', form).addEventListener('click', () => {
            const from = state.source;
            const to = otherSource(from);
            if (!window.confirm(`Replace the ${SOURCE_LABELS[to]} Priority, ADX IDs, Fullscreen sequence and Per-screen ads with the ${SOURCE_LABELS[from]} values?`)) return;
            const data = gatherSource(from);
            renderAdx(to, data.adx);
            PRIORITY_TYPES.forEach((type) => {
                $(`[data-key="${to}.priority.${type}.main"]`, form).value = data.priority[type].main;
                $(`[data-key="${to}.priority.${type}.failed"]`, form).value = data.priority[type].failed;
            });
            state.sequence[to] = data.fullscreen_sequence.slice();
            renderSequence(to);
            renderScreens(to, data.screen_ads);
            toast(`Copied to ${SOURCE_LABELS[to]}. Click Save to apply.`);
        });

        // Per-screen ads (the list of screens is the same for both sources, the switches are not)
        const SCREEN_FLAGS = [['native_big', 'Native Big'], ['native_small', 'Native Small'], ['banner', 'Banner'], ['fullscreen', 'Fullscreen']];
        const screenRows = (source) => $('[data-screen-rows]', panel(source));

        function screenRowHtml(screen) {
            const supported = screen.flags || SCREEN_FLAGS.map(([flag]) => flag);
            const toggles = SCREEN_FLAGS.map(([flag, label]) => supported.includes(flag) ? `
                <td data-label="${label}">
                    <label class="switch" aria-label="${esc(screen.label)} ${label}">
                        <input type="checkbox" data-flag="${flag}"${screen[flag] ? ' checked' : ''}>
                        <span class="slider"></span>
                    </label>
                </td>` : `<td data-label="${label}" class="muted">&mdash;</td>`).join('');
            return `<tr data-screen-key="${esc(screen.screen_key)}" data-screen-flags="${esc(supported.join(','))}">
                <td data-label="Screen"><strong>${esc(screen.label)}</strong><span class="screen-key">${esc(screen.screen_key)}</span></td>
                ${toggles}
            </tr>`;
        }

        function renderScreens(source, screens) {
            screenRows(source).innerHTML = (screens || []).map(screenRowHtml).join('');
        }

        function gatherScreens(source) {
            return $$('tr', screenRows(source)).map((tr) => {
                const screen = { screen_key: tr.dataset.screenKey, flags: tr.dataset.screenFlags.split(',') };
                $$('[data-flag]', tr).forEach((input) => { screen[input.dataset.flag] = input.checked; });
                return screen;
            });
        }

        function render(data) {
            renderUnits('admob', data.admob);
            renderUnits('tradplus', data.tradplus);
            $('[data-key="custom.enabled"]', form).checked = Boolean(data.custom.enabled);
            $('[data-key="custom.url"]', form).value = data.custom.url || '';
            SOURCES.forEach((source) => {
                const s = (data.by_source || {})[source] || {};
                renderAdx(source, s.adx);
                renderPriority(source, s.priority);
                state.sequence[source] = (s.fullscreen_sequence || []).slice();
                renderSequence(source);
                renderScreens(source, s.screen_ads);
            });
            markSourceErrors({});
        }

        function gatherSource(source) {
            const root = panel(source);
            const data = { priority: {}, adx: {}, fullscreen_sequence: state.sequence[source].slice(), screen_ads: gatherScreens(source) };
            PRIORITY_TYPES.forEach((type) => {
                data.priority[type] = {
                    main: $(`[data-key="${source}.priority.${type}.main"]`, root).value.trim(),
                    failed: $(`[data-key="${source}.priority.${type}.failed"]`, root).value.trim(),
                };
            });
            $$('.adx-group', root).forEach((group) => {
                data.adx[group.dataset.adxType] = $$('.adx-row', group).map((row) => ({
                    id: $('[data-field="id"]', row).value.trim(),
                    weight: $('[data-field="weight"]', row).value.trim(),
                }));
            });
            return data;
        }

        function gather() {
            const body = { admob: {}, tradplus: {}, custom: {}, by_source: {} };
            SOURCES.forEach((source) => { body.by_source[source] = gatherSource(source); });
            $$('[data-units] [data-key]', form).forEach((el) => {
                const [provider, type, field] = el.dataset.key.split('.');
                body[provider][type] = body[provider][type] || {};
                body[provider][type][field] = el.type === 'checkbox' ? el.checked : el.value.trim();
            });
            body.custom = {
                enabled: $('[data-key="custom.enabled"]', form).checked,
                url: $('[data-key="custom.url"]', form).value.trim(),
            };
            return body;
        }

        function validate() {
            const body = gather();
            const errors = {};
            ['admob', 'tradplus'].forEach((provider) => {
                Object.entries(body[provider]).forEach(([type, unit]) => {
                    if (!NO_SPACE_RE.test(unit.id)) errors[`${provider}.${type}.id`] = 'Ad unit ID cannot contain spaces';
                    else if (unit.enabled && !unit.id) errors[`${provider}.${type}.id`] = 'Enter an ad unit ID or turn this off';
                });
            });
            if (body.custom.url && !isValidUrl(body.custom.url)) errors['custom.url'] = 'Enter a valid URL (https://...)';
            else if (body.custom.enabled && !body.custom.url) errors['custom.url'] = 'Custom Ads URL is required when enabled';
            Object.entries(body.by_source).forEach(([source, data]) => {
                Object.entries(data.adx).forEach(([type, rows]) => {
                    rows.forEach((row, index) => {
                        const key = `${source}.adx.${type}.${index}`;
                        if (!row.id) errors[`${key}.id`] = 'Enter an ad unit ID or remove the row';
                        else if (!NO_SPACE_RE.test(row.id)) errors[`${key}.id`] = 'Ad unit ID cannot contain spaces';
                        if (!/^\d+$/.test(row.weight)) {
                            errors[`${key}.weight`] = row.weight.startsWith('-') ? 'Weight cannot be negative' : 'Weight must be a whole number';
                        }
                    });
                });
                Object.entries(data.priority).forEach(([type, p]) => {
                    const key = `${source}.priority.${type}`;
                    const main = parsePriority(p.main, true);
                    if (main === null) errors[`${key}.main`] = `Use numbers 1-4 (up to ${PRIORITY_SEQUENCE_MAX}), e.g. 1,1,2,3,1`;
                    else if (!main.length) errors[`${key}.main`] = 'Enter at least one network, e.g. 1,1,2,3,1';
                    if (parsePriority(p.failed) === null) errors[`${key}.failed`] = 'Use numbers 1-4 without repeats, e.g. 3,4';
                });
            });
            return errors;
        }

        async function load() {
            setSectionLoading(form, true);
            try {
                const { data } = await api('ads/get_config.php');
                render(data);
            } catch (err) {
                toast(err.message, 'error');
            } finally {
                setSectionLoading(form, false);
            }
        }

        bindSubmit(form, {
            validate,
            onErrors: markSourceErrors,
            async save() {
                const json = await api('ads/save_config.php', { method: 'POST', body: gather() });
                render(json.data);
                toast('Ads configuration saved');
            },
        });
        load();
    };

    pages.users = function () {
        const state = { page: 1, source: '', search: '', pages: 1 };
        let searchTimer = null;

        async function load() {
            const params = new URLSearchParams({ page: state.page, source: state.source, search: state.search });
            const tbody = $('#userRows');
            tbody.innerHTML = '<tr><td colspan="5" class="table-empty"><span class="spinner"></span></td></tr>';
            try {
                const { data } = await api(`users/user_stats.php?${params}`);
                ['organic', 'marketing', 'total'].forEach((key) => {
                    $(`[data-stat="${key}"]`).textContent = formatNumber(data.summary[key]);
                });
                state.pages = data.pages;
                tbody.innerHTML = data.users.length ? data.users.map((u) => `
                    <tr>
                        <td data-label="Device ID"><span class="mono">${esc(u.device_id)}</span></td>
                        <td data-label="Source"><span class="badge badge-${esc(u.source)}">${esc(u.source)}</span></td>
                        <td data-label="App version">${esc(u.app_version || '–')}</td>
                        <td data-label="Installed">${esc(formatDate(u.created_at))}</td>
                        <td data-label="Last seen">${esc(formatDate(u.last_seen_at))}</td>
                    </tr>`).join('') : '<tr><td colspan="5" class="table-empty">No users found</td></tr>';
                $('#pageInfo').textContent = `Page ${data.page} of ${data.pages} · ${formatNumber(data.total)} users`;
                $('#prevPage').disabled = data.page <= 1;
                $('#nextPage').disabled = data.page >= data.pages;
            } catch (err) {
                tbody.innerHTML = '<tr><td colspan="5" class="table-empty">Could not load users</td></tr>';
                toast(err.message, 'error');
            }
        }

        $('#userSource').addEventListener('change', (e) => {
            state.source = e.target.value;
            state.page = 1;
            load();
        });
        $('#userSearch').addEventListener('input', (e) => {
            clearTimeout(searchTimer);
            searchTimer = setTimeout(() => {
                state.search = e.target.value.trim();
                state.page = 1;
                load();
            }, 300);
        });
        $('#prevPage').addEventListener('click', () => { if (state.page > 1) { state.page--; load(); } });
        $('#nextPage').addEventListener('click', () => { if (state.page < state.pages) { state.page++; load(); } });
        load();
    };

    pages.facebook = function () {
        const form = $('#facebookForm');

        function apply(data) {
            fillForm(form, {
                facebook_app_id: data.facebook_app_id,
                facebook_client_token: data.facebook_client_token,
                facebook_secret_key: '',
                clear_secret: false,
            });
            $('#secretHint').textContent = data.facebook_secret_set
                ? 'A secret key is saved. Leave empty to keep it.'
                : 'No secret key saved yet.';
        }

        async function load() {
            setSectionLoading(form, true);
            try {
                const { data } = await api('marketing/facebook_config.php');
                apply(data);
            } catch (err) {
                toast(err.message, 'error');
            } finally {
                setSectionLoading(form, false);
            }
        }

        bindSubmit(form, {
            validate() {
                const d = readForm(form);
                const errors = {};
                if (d.facebook_app_id && !/^\d{5,30}$/.test(d.facebook_app_id)) errors.facebook_app_id = 'Facebook App ID contains digits only';
                if (d.facebook_client_token && !/^[A-Za-z0-9]{8,100}$/.test(d.facebook_client_token)) errors.facebook_client_token = 'Client token contains letters and digits only';
                if (d.facebook_secret_key && !/^\S{8,255}$/.test(d.facebook_secret_key)) errors.facebook_secret_key = 'Secret key cannot contain spaces';
                return errors;
            },
            async save() {
                const json = await api('marketing/facebook_config.php', { method: 'POST', body: readForm(form) });
                apply(json.data);
                toast('Facebook settings saved');
            },
        });
        load();
    };

    const PUSH_IMAGE_MAX_BYTES = 2 * 1024 * 1024;
    const PUSH_TARGET_LABELS = { all: 'All users', organic: 'Organic', marketing: 'Marketing' };

    pages.notifications = function () {
        const firebaseForm = $('#firebaseForm');
        const form = $('#notificationForm');
        const tbody = $('#pushRows');

        function showFirebase(data) {
            $('#firebaseStatus').textContent = data.set
                ? `Saved: ${data.project_id} (${data.client_email}). Upload a new file to replace it.`
                : 'No service account saved yet. Upload it before sending notifications.';
        }

        async function loadFirebase() {
            try {
                const { data } = await api('notifications/credentials.php');
                showFirebase(data);
            } catch (err) {
                $('#firebaseStatus').textContent = 'Could not load the Firebase configuration';
                toast(err.message, 'error');
            }
        }

        async function loadHistory() {
            tbody.innerHTML = '<tr><td colspan="6" class="table-empty"><span class="spinner"></span></td></tr>';
            try {
                const { data } = await api('notifications/history.php');
                $$('[data-reach]').forEach((el) => { el.textContent = formatNumber(data.reachable[el.dataset.reach]); });
                tbody.innerHTML = data.history.length ? data.history.map((n) => `
                    <tr>
                        <td data-label="Notification"><strong>${esc(n.title)}</strong><br><span class="muted">${esc(n.body)}</span>${n.link ? `<br><span class="mono">${esc(n.link)}</span>` : ''}</td>
                        <td data-label="Sent to">${esc(PUSH_TARGET_LABELS[n.target] || n.target)}</td>
                        <td data-label="Users">${formatNumber(n.total_count)}</td>
                        <td data-label="Delivered">${formatNumber(n.success_count)}</td>
                        <td data-label="Failed">${formatNumber(n.failure_count)}</td>
                        <td data-label="Date">${esc(formatDate(n.created_at))}</td>
                    </tr>`).join('') : '<tr><td colspan="6" class="table-empty">No notifications sent yet</td></tr>';
            } catch (err) {
                tbody.innerHTML = '<tr><td colspan="6" class="table-empty">Could not load notifications</td></tr>';
                toast(err.message, 'error');
            }
        }

        bindSubmit(firebaseForm, {
            validate() {
                const file = $('#service_account').files[0];
                if (!file) return { service_account: 'Choose the service account JSON file' };
                return {};
            },
            async save() {
                const body = new FormData();
                body.append('service_account', $('#service_account').files[0]);
                const json = await api('notifications/credentials.php', { method: 'POST', body });
                showFirebase(json.data);
                firebaseForm.reset();
                toast('Firebase service account saved');
            },
        });

        bindSubmit(form, {
            validate() {
                const d = readForm(form);
                const errors = {};
                if (!d.title) errors.title = 'Title is required';
                if (!d.body) errors.body = 'Description is required';
                if (d.link && !isValidUrl(d.link)) errors.link = 'Enter a valid URL (https://...)';
                const image = $('#push_image').files[0];
                if (image && !['image/jpeg', 'image/png', 'image/webp'].includes(image.type)) errors.image = 'Use a JPG, PNG or WebP image';
                else if (image && image.size > PUSH_IMAGE_MAX_BYTES) errors.image = 'The image must be 2 MB or smaller';
                return errors;
            },
            async save() {
                const d = readForm(form);
                const body = new FormData();
                ['title', 'body', 'link', 'target'].forEach((key) => body.append(key, d[key]));
                const image = $('#push_image').files[0];
                if (image) body.append('image', image);
                const json = await api('notifications/send.php', { method: 'POST', body });
                form.reset();
                toast(json.message);
                loadHistory();
            },
        });

        loadFirebase();
        loadHistory();
    };

    document.addEventListener('DOMContentLoaded', () => {
        initChrome();
        const page = pages[document.body.dataset.page];
        if (page) page();
    });
})();

/*
 * Guided tour for the technical demo pages.
 *
 * Mark the elements to walk through, in order:
 *
 *   <section data-tour="1" data-tour-title="What the LMS sent">
 *     <template data-tour-text><p>Explanation, may contain <code>markup</code>.</p></template>
 *     ...
 *   </section>
 *
 * Each step dims the rest of the page and blocks clicks outside the highlighted element, which stays usable
 * (so the last step can point at the real Continue button). Steps inside a closed <details> open it.
 *
 * The tour starts by itself when <body data-tour-autostart="true">, unless the viewer turned that off
 * (remembered in localStorage when available). Any element with [data-tour-start] restarts it.
 */
(function () {
    'use strict';

    var STORAGE_KEY = 'lti-demo-tour-autostart';
    var PAD = 6;

    function readAutostartPreference() {
        try {
            return window.localStorage.getItem(STORAGE_KEY) !== 'off';
        } catch (e) {
            return true;
        }
    }

    function writeAutostartPreference(on) {
        try {
            window.localStorage.setItem(STORAGE_KEY, on ? 'on' : 'off');
        } catch (e) {
            // Storage can be blocked inside an LMS iframe; the checkbox then only lasts for this page.
        }
    }

    function collectSteps() {
        return Array.prototype.slice.call(document.querySelectorAll('[data-tour]'))
            .filter(function (el) {
                // Skip steps that are not rendered (for example the client-mode view hides the technical one).
                return el.getClientRects().length > 0 || el.closest('details');
            })
            .sort(function (a, b) {
                return parseFloat(a.getAttribute('data-tour')) - parseFloat(b.getAttribute('data-tour'));
            });
    }

    function stepText(el) {
        var tpl = el.querySelector(':scope > template[data-tour-text]');
        if (tpl) {
            return tpl.innerHTML;
        }
        var text = el.getAttribute('data-tour-text') || '';
        var p = document.createElement('p');
        p.textContent = text;
        return p.outerHTML;
    }

    function prefersReducedMotion() {
        return window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    }

    var tour = {
        steps: [],
        index: 0,
        active: false,
        nodes: null,
        lastFocus: null,

        start: function (fromIndex) {
            this.steps = collectSteps();
            if (this.steps.length === 0) {
                return;
            }
            this.index = Math.max(0, Math.min(fromIndex || 0, this.steps.length - 1));
            this.lastFocus = document.activeElement;
            this.build();
            this.active = true;
            this.show();
        },

        stop: function () {
            if (!this.active) {
                return;
            }
            this.active = false;
            this.nodes.root.remove();
            this.nodes = null;
            if (this.observer) {
                this.observer.disconnect();
                this.observer = null;
            }
            window.removeEventListener('resize', this.onViewportChange);
            window.removeEventListener('scroll', this.onViewportChange, true);
            document.removeEventListener('keydown', this.onKey);
            if (this.lastFocus && this.lastFocus.focus) {
                this.lastFocus.focus();
            }
        },

        build: function () {
            var root = document.createElement('div');
            root.className = 'tour-root';
            var masks = ['top', 'right', 'bottom', 'left'].map(function () {
                var m = document.createElement('div');
                m.className = 'tour-mask';
                root.appendChild(m);
                return m;
            });
            var ring = document.createElement('div');
            ring.className = 'tour-ring';
            root.appendChild(ring);

            var pop = document.createElement('div');
            pop.className = 'tour-pop';
            pop.setAttribute('role', 'dialog');
            pop.setAttribute('aria-modal', 'false');
            pop.setAttribute('aria-labelledby', 'tour-title');
            pop.tabIndex = -1;
            pop.innerHTML =
                '<p class="tour-count" id="tour-count"></p>' +
                '<h2 class="tour-title" id="tour-title"></h2>' +
                '<div class="tour-text" id="tour-text"></div>' +
                '<div class="tour-foot">' +
                '  <button type="button" class="tour-skip" data-tour-action="close">End tour</button>' +
                '  <div class="demo-actions">' +
                '    <button type="button" class="demo-button secondary small" data-tour-action="back">Back</button>' +
                '    <button type="button" class="demo-button small" data-tour-action="next">Next</button>' +
                '  </div>' +
                '</div>' +
                '<label class="tour-auto"><input type="checkbox" data-tour-action="auto"> Start the tour automatically on each step</label>';
            root.appendChild(pop);
            document.body.appendChild(root);

            var auto = pop.querySelector('[data-tour-action="auto"]');
            auto.checked = readAutostartPreference();

            var self = this;
            pop.addEventListener('click', function (event) {
                var action = event.target.getAttribute('data-tour-action');
                if (action === 'next') { self.next(); }
                if (action === 'back') { self.back(); }
                if (action === 'close') { self.stop(); }
            });
            auto.addEventListener('change', function () {
                writeAutostartPreference(auto.checked);
            });
            // Clicking the dimmed area does nothing on purpose: the point is to keep attention on the step.

            this.onViewportChange = function () { self.position(); };
            this.onKey = function (event) {
                if (event.key === 'Escape') { self.stop(); }
                if (event.target && /^(INPUT|TEXTAREA|SELECT)$/.test(event.target.tagName)) { return; }
                if (event.key === 'ArrowRight') { self.next(); }
                if (event.key === 'ArrowLeft') { self.back(); }
            };
            window.addEventListener('resize', this.onViewportChange);
            window.addEventListener('scroll', this.onViewportChange, true);
            document.addEventListener('keydown', this.onKey);
            // Layout can change without a resize event (fonts loading, a <details> opening, the LMS resizing its
            // iframe), so also re-measure whenever the page or the highlighted element changes size.
            this.observer = window.ResizeObserver ? new ResizeObserver(this.onViewportChange) : null;
            if (this.observer) {
                this.observer.observe(document.documentElement);
            }

            this.nodes = { root: root, masks: masks, ring: ring, pop: pop };
        },

        current: function () {
            return this.steps[this.index];
        },

        show: function () {
            var el = this.current();
            if (this.observer) {
                if (this.observed) {
                    this.observer.unobserve(this.observed);
                }
                this.observer.observe(el);
                this.observed = el;
            }
            var details = el.tagName === 'DETAILS' ? el : el.closest('details');
            if (details && !details.open) {
                details.open = true;
            }
            var n = this.nodes;
            n.pop.querySelector('#tour-count').textContent = 'Step ' + (this.index + 1) + ' of ' + this.steps.length;
            n.pop.querySelector('#tour-title').textContent = el.getAttribute('data-tour-title') || '';
            n.pop.querySelector('#tour-text').innerHTML = stepText(el);
            n.pop.querySelector('[data-tour-action="back"]').disabled = this.index === 0;
            n.pop.querySelector('[data-tour-action="next"]').textContent =
                this.index === this.steps.length - 1 ? 'Finish' : 'Next';

            el.scrollIntoView({ block: 'center', behavior: prefersReducedMotion() ? 'auto' : 'smooth' });
            this.position();
            var self = this;
            // Smooth scrolling moves the target after this call, so settle the position again.
            window.setTimeout(function () { if (self.active) { self.position(); } }, 350);
            n.pop.focus({ preventScroll: true });
        },

        position: function () {
            if (!this.active || !this.nodes) {
                return;
            }
            var el = this.current();
            var r = el.getBoundingClientRect();
            var vw = document.documentElement.clientWidth;
            var vh = document.documentElement.clientHeight;
            var top = Math.max(0, r.top - PAD);
            var left = Math.max(0, r.left - PAD);
            var right = Math.min(vw, r.right + PAD);
            var bottom = Math.min(vh, r.bottom + PAD);
            var m = this.nodes.masks;

            // Four panels around the highlighted box: they dim and block everything else.
            setBox(m[0], 0, 0, vw, top);
            setBox(m[1], right, top, vw - right, bottom - top);
            setBox(m[2], 0, bottom, vw, vh - bottom);
            setBox(m[3], 0, top, left, bottom - top);
            setBox(this.nodes.ring, left, top, right - left, bottom - top);

            // Popover: below the target if it fits, else above, else pinned to the bottom of the viewport.
            var pop = this.nodes.pop;
            var ph = pop.offsetHeight;
            var pw = pop.offsetWidth;
            var gap = 12;
            var y;
            if (bottom + gap + ph <= vh) {
                y = bottom + gap;
            } else if (top - gap - ph >= 0) {
                y = top - gap - ph;
            } else {
                y = vh - ph - gap;
            }
            var x = Math.min(Math.max(12, left), vw - pw - 12);
            pop.style.top = Math.max(12, y) + 'px';
            pop.style.left = x + 'px';
        },

        next: function () {
            if (this.index < this.steps.length - 1) {
                this.index += 1;
                this.show();
            } else {
                this.stop();
            }
        },

        back: function () {
            if (this.index > 0) {
                this.index -= 1;
                this.show();
            }
        }
    };

    function setBox(node, x, y, w, h) {
        node.style.left = x + 'px';
        node.style.top = y + 'px';
        node.style.width = Math.max(0, w) + 'px';
        node.style.height = Math.max(0, h) + 'px';
    }

    document.addEventListener('click', function (event) {
        var trigger = event.target.closest && event.target.closest('[data-tour-start]');
        if (trigger) {
            tour.stop();
            tour.start(0);
        }
    });

    function autostart() {
        if (document.body.getAttribute('data-tour-autostart') === 'true' && readAutostartPreference()) {
            // Give the page a moment to lay out (and async checks a moment to update their status lines).
            window.setTimeout(function () { tour.start(0); }, 300);
        }
    }

    window.demoTour = {
        start: function (index) { tour.start(index); },
        stop: function () { tour.stop(); },
        refresh: function () { tour.position(); }
    };

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', autostart);
    } else {
        autostart();
    }
})();

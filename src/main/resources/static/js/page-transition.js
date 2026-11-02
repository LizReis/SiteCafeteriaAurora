/**
 * Transição global entre páginas do Café Aurora.
 * Exibe o vídeo curto e centralizado somente durante navegações internas.
 * Não intercepta links externos, fragmentos na mesma página ou atalhos do navegador.
 */
(() => {
  'use strict';

  const VIDEO_URL = '/videos/video-carregamento.mp4';
  const MINIMUM_DISPLAY_MS = 480; // Discreto: menos de meio segundo de espera visual.
  const MAXIMUM_DISPLAY_MS = 8000;
  let overlay;
  let video;
  let navigating = false;
  let navigationTimer;
  let safetyTimer;

  function createOverlay() {
    if (overlay) return;
    overlay = document.createElement('div');
    overlay.className = 'aurora-page-transition';
    overlay.setAttribute('role', 'status');
    overlay.setAttribute('aria-label', 'Carregando página do Café Aurora');
    overlay.setAttribute('aria-live', 'polite');
    overlay.setAttribute('aria-hidden', 'true');

    const content = document.createElement('div');
    content.className = 'aurora-loader-content';

    const frame = document.createElement('div');
    frame.className = 'aurora-loader-frame';

    video = document.createElement('video');
    video.className = 'aurora-loader-video';
    video.src = VIDEO_URL;
    video.preload = 'auto';
    video.muted = true;
    video.defaultMuted = true;
    video.loop = true;
    video.playsInline = true;
    video.setAttribute('playsinline', '');
    video.setAttribute('aria-hidden', 'true');

    const fallback = document.createElement('span');
    fallback.className = 'aurora-loader-fallback';
    fallback.textContent = 'Café Aurora';
    fallback.setAttribute('aria-hidden', 'true');

    const caption = document.createElement('p');
    caption.className = 'aurora-loader-caption';
    caption.textContent = 'Preparando sua experiência...';

    frame.append(video, fallback);
    content.append(frame, caption);
    overlay.append(content);
    document.body.append(overlay);

    video.addEventListener('error', () => overlay.classList.add('video-unavailable'));
  }

  function show() {
    createOverlay();
    const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    overlay.classList.toggle('reduced-motion', reducedMotion);
    overlay.classList.add('is-visible');
    overlay.setAttribute('aria-hidden', 'false');
    if (!reducedMotion && !video.error) {
      try {
        video.currentTime = 0;
        const result = video.play();
        result?.catch?.(() => overlay.classList.add('video-unavailable'));
      } catch (_) {
        overlay.classList.add('video-unavailable');
      }
    }
  }

  function hide() {
    window.clearTimeout(navigationTimer);
    window.clearTimeout(safetyTimer);
    navigating = false;
    if (!overlay) return;
    overlay.classList.remove('is-visible');
    overlay.setAttribute('aria-hidden', 'true');
    video?.pause();
  }

  function navigate(url) {
    if (navigating) return;
    navigating = true;
    show();
    navigationTimer = window.setTimeout(() => {
      window.location.assign(url);
    }, window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 180 : MINIMUM_DISPLAY_MS);
    // Se o navegador impedir a navegação, a tela jamais fica bloqueada indefinidamente.
    safetyTimer = window.setTimeout(hide, MAXIMUM_DISPLAY_MS);
  }

  function isSamePageAnchor(url) {
    return url.pathname === window.location.pathname
      && url.search === window.location.search
      && url.hash !== '';
  }

  function onLinkClick(event) {
    if (event.defaultPrevented || event.button !== 0 ||
        event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return;
    const link = event.target instanceof Element ? event.target.closest('a[href]') : null;
    if (!link || link.hasAttribute('download') ||
        (link.target && link.target !== '_self')) return;

    let destination;
    try { destination = new URL(link.href, window.location.href); }
    catch (_) { return; }

    if (!['http:', 'https:'].includes(destination.protocol) ||
        destination.origin !== window.location.origin ||
        destination.href === window.location.href ||
        isSamePageAnchor(destination)) return;

    event.preventDefault();
    navigate(destination.href);
  }

  function init() {
    createOverlay();
    document.addEventListener('click', onLinkClick);
    window.addEventListener('pageshow', hide);
    // A aplicação pode iniciar a transição também após validar um formulário.
    window.CafeAuroraPageLoader = Object.freeze({ show, hide, navigate });
  }

  if (document.readyState === 'loading')
    document.addEventListener('DOMContentLoaded', init, { once: true });
  else init();
})();

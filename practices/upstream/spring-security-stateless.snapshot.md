<!DOCTYPE html>
<html lang="en">
  <script src="https://cdn.cookielaw.org/scripttemplates/otSDKStub.js" data-domain-script="018ee325-b3a7-7753-937b-b8b3e643b1a7"></script><script>function OptanonWrapper() {}</script><script>function setGTM(w, d, s, l, i) { w[l] = w[l] || []; w[l].push({ "gtm.start": new Date().getTime(), event: "gtm.js"}); var f = d.getElementsByTagName(s)[0], j = d.createElement(s), dl = l != "dataLayer" ? "&l=" + l : ""; j.async = true; j.src = "https://www.googletagmanager.com/gtm.js?id=" + i + dl; f.parentNode.insertBefore(j, f); } if (document.cookie.indexOf("OptanonConsent") > -1 && document.cookie.indexOf("groups=") > -1) { setGTM(window, document, "script", "dataLayer", "GTM-W8CQ8TL"); } else { waitForOnetrustActiveGroups(); } var timer; function waitForOnetrustActiveGroups() { if (document.cookie.indexOf("OptanonConsent") > -1 && document.cookie.indexOf("groups=") > -1) { clearTimeout(timer); setGTM(window, document, "script", "dataLayer", "GTM-W8CQ8TL"); } else { timer = setTimeout(waitForOnetrustActiveGroups, 250); }}</script>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>Authentication Persistence and Session Management :: Spring Security</title>
    <link rel="canonical" href="https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html">
    <meta name="generator" content="Antora 3.2.0-rc.2">
    <script>
!function (theme, navWidth) {
  if (theme === 'dark') document.documentElement.classList.add('dark-theme')
  if (navWidth) document.documentElement.style.setProperty('--nav-width', `${navWidth}px`)
}(localStorage && localStorage.getItem('theme') || (matchMedia('(prefers-color-scheme: dark)')?.matches && 'dark'),
  localStorage && localStorage.getItem('nav-width'))
    </script>
    <link rel="stylesheet" href="../../_/css/site.css">
    <link rel="stylesheet" href="../../_/css/vendor/search.css">
    <link rel="stylesheet" href="../../_/css/vendor/page-search.css">
    <link rel="stylesheet" href="../../_/css/vendor/onetrust.css">
    <link rel="stylesheet" href="../../_/css/vendor/asciidoctor-tabs.css">

    <meta name="antora-ui-version" content="v0.4.26"> 
    <meta name="version" content="7.1.1">
    <meta name="generation" content="7.1">
    <meta name="versioned-url" content="https://docs.spring.io/spring-security/reference/7.1/servlet/authentication/session-management.html">
    <meta name="component" content="security">
    <meta name="latest-version" content="true">
    <link rel="icon" href="../../_/img/favicon.ico" type="image/vnd.microsoft.icon">
  </head>
  <body class="article">
<header class="header">
  <nav class="navbar">
    <div class="navbar-brand">
      <a class="navbar-item" href="https://spring.io">
        <img
          id="springlogo"
          class="block"
          src="../../_/img/spring-logo.svg"
          alt="Spring"
        />
      </a>
      <button class="navbar-burger" data-target="topbar-nav">
        <span></span>
        <span></span>
        <span></span>
      </button>
    </div>
    <div id="topbar-nav" class="navbar-menu">
      <div class="navbar-end">
        <div class="navbar-item has-dropdown is-hoverable">
          <a class="navbar-link" href="#">Why Spring</a>
          <div class="navbar-dropdown">
            <a class="navbar-item" href="https://spring.io/why-spring">Overview</a>
            <li class="navbar-item navbar-item-special-3">Trending</li>
            <a class="navbar-item" href="https://spring.io/ai">Generative AI</a>
            <a class="navbar-item" href="https://spring.io/cloud">Cloud</a>
            <li class="navbar-item navbar-item-special-3">Architecture Patterns</li>
            <a class="navbar-item" href="https://spring.io/microservices">Microservices</a>
            <a class="navbar-item" href="https://spring.io/reactive">Reactive</a>
            <a class="navbar-item" href="https://spring.io/event-driven">Event Driven</a>
            <li class="navbar-item navbar-item-special-3">Application Types</li>
            <a class="navbar-item" href="https://spring.io/web-applications">Web Applications</a>
            <a class="navbar-item" href="https://spring.io/serverless">Serverless</a>
            <a class="navbar-item" href="https://spring.io/batch">Batch</a>
          </div>
        </div>

        <div class="navbar-item has-dropdown is-hoverable">
          <a class="navbar-link" href="#">Learn</a>
          <div class="navbar-dropdown">
            <li class="navbar-item navbar-item-special-3">Getting Started</li>
            <a class="navbar-item" href="https://spring.io/quickstart">Quickstart</a>
            <a class="navbar-item" href="https://spring.io/guides">Guides</a>
            <li class="navbar-item navbar-item-special-3">Academy</li>
            <a class="navbar-item" href="https://spring.academy/courses">Courses
              <svg class="external-link-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 16 16"><polyline points="15 10.94 15 15 1 15 1 1 5.06 1" fill="none" stroke="currentColor" stroke-miterlimit="10" stroke-width="2"></polyline><polyline points="8.93 1 15 1 15 7.07" fill="none" stroke="currentColor" stroke-miterlimit="10" stroke-width="2"></polyline><line x1="15" y1="1" x2="8" y2="8" fill="none" stroke="currentColor" stroke-miterlimit="10" stroke-width="2"></line></svg>
            </a>
            <a class="navbar-item" href="https://spring.academy/learning-path">Get Certified
              <svg class="external-link-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 16 16"><polyline points="15 10.94 15 15 1 15 1 1 5.06 1" fill="none" stroke="currentColor" stroke-miterlimit="10" stroke-width="2"></polyline><polyline points="8.93 1 15 1 15 7.07" fill="none" stroke="currentColor" stroke-miterlimit="10" stroke-width="2"></polyline><line x1="15" y1="1" x2="8" y2="8" fill="none" stroke="currentColor" stroke-miterlimit="10" stroke-width="2"></line></svg>
            </a>
          </div>
        </div>

        <div class="navbar-item has-dropdown is-hoverable">
          <a class="navbar-link" href="#">Projects</a>
          <div class="navbar-dropdown" style="min-width: 280px">
            <a class="navbar-item" href="https://spring.io/projects">Overview</a>
            <li class="navbar-item navbar-item-special-3">Projects</li>
            <a class="navbar-item" href="https://spring.io/projects/spring-boot">Spring Boot</a>
            <a class="navbar-item" href="https://spring.io/projects/spring-framework">Spring Framework</a>
            <a class="navbar-item" href="https://spring.io/projects/spring-cloud">Spring Cloud</a>
            <a class="navbar-item" href="https://spring.io/projects/spring-ai">Spring AI</a>
            <a class="navbar-item" href="https://spring.io/projects/spring-data">Spring Data</a>
            <a class="navbar-item" href="https://spring.io/projects/spring-integration">Spring Integration</a>
            <a class="navbar-item" href="https://spring.io/projects/spring-batch">Spring Batch</a>
            <a class="navbar-item" href="https://spring.io/projects/spring-security">Spring Security</a>
            <li class="navbar-item navbar-item-special-3">Foundational Projects</li>
            <a class="navbar-item" href="https://micrometer.io">Micrometer
              <svg class="external-link-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 16 16"><polyline points="15 10.94 15 15 1 15 1 1 5.06 1" fill="none" stroke="currentColor" stroke-miterlimit="10" stroke-width="2"></polyline><polyline points="8.93 1 15 1 15 7.07" fill="none" stroke="currentColor" stroke-miterlimit="10" stroke-width="2"></polyline><line x1="15" y1="1" x2="8" y2="8" fill="none" stroke="currentColor" stroke-miterlimit="10" stroke-width="2"></line></svg>
            </a>
            <a class="navbar-item" href="https://projectreactor.io">Reactor
              <svg class="external-link-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 16 16"><polyline points="15 10.94 15 15 1 15 1 1 5.06 1" fill="none" stroke="currentColor" stroke-miterlimit="10" stroke-width="2"></polyline><polyline points="8.93 1 15 1 15 7.07" fill="none" stroke="currentColor" stroke-miterlimit="10" stroke-width="2"></polyline><line x1="15" y1="1" x2="8" y2="8" fill="none" stroke="currentColor" stroke-miterlimit="10" stroke-width="2"></line></svg>
            </a>
            <li class="navbar-item navbar-item-special-3">Development Tools</li>
            <a class="navbar-item" href="https://spring.io/tools">Spring Tools</a>
            <a class="navbar-item navbar-item-special-2" href="https://start.spring.io">Spring Initializr
              <svg class="external-link-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 16 16"><polyline points="15 10.94 15 15 1 15 1 1 5.06 1" fill="none" stroke="currentColor" stroke-miterlimit="10" stroke-width="2"></polyline><polyline points="8.93 1 15 1 15 7.07" fill="none" stroke="currentColor" stroke-miterlimit="10" stroke-width="2"></polyline><line x1="15" y1="1" x2="8" y2="8" fill="none" stroke="currentColor" stroke-miterlimit="10" stroke-width="2"></line></svg>
            </a>
          </div>
        </div>

        <div class="navbar-item has-dropdown is-hoverable">
          <a class="navbar-link" href="#">Resources</a>
          <div class="navbar-dropdown">
            <a class="navbar-item" href="https://spring.io/blog">Blog</a>
            <a class="navbar-item" href="https://spring.io/projects#release-calendar">Release Calendar</a>
            <a class="navbar-item" href="https://spring.io/projects/generations">Version Mappings</a>
            <a class="navbar-item" href="https://spring.io/projects/release-highlights">Release Highlights</a>
            <a class="navbar-item" href="https://spring.io/security">Security Advisories</a>
            <li class="navbar-item navbar-item-special-3">GitHub Orgs</li>
            <a class="navbar-item" href="https://github.com/spring-projects">Spring Projects
              <svg class="external-link-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 16 16"><polyline points="15 10.94 15 15 1 15 1 1 5.06 1" fill="none" stroke="currentColor" stroke-miterlimit="10" stroke-width="2"></polyline><polyline points="8.93 1 15 1 15 7.07" fill="none" stroke="currentColor" stroke-miterlimit="10" stroke-width="2"></polyline><line x1="15" y1="1" x2="8" y2="8" fill="none" stroke="currentColor" stroke-miterlimit="10" stroke-width="2"></line></svg>
            </a>
            <a class="navbar-item" href="https://github.com/spring-cloud">Spring Cloud
              <svg class="external-link-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 16 16"><polyline points="15 10.94 15 15 1 15 1 1 5.06 1" fill="none" stroke="currentColor" stroke-miterlimit="10" stroke-width="2"></polyline><polyline points="8.93 1 15 1 15 7.07" fill="none" stroke="currentColor" stroke-miterlimit="10" stroke-width="2"></polyline><line x1="15" y1="1" x2="8" y2="8" fill="none" stroke="currentColor" stroke-miterlimit="10" stroke-width="2"></line></svg>
            </a>
          </div>
        </div>

        <div class="navbar-item has-dropdown is-hoverable">
          <a class="navbar-link" href="#">Community</a>
          <div class="navbar-dropdown">
            <a class="navbar-item" href="https://spring.io/community">Overview</a>
            <a class="navbar-item" href="https://spring.io/events">Events</a>
            <a class="navbar-item" href="https://spring.io/authors">Authors</a>
          </div>
        </div>

        <div class="navbar-item has-dropdown is-hoverable is-enterprise">
          <a class="navbar-link" href="#">Enterprise</a>
          <div class="navbar-dropdown lg is-right">
            <a class="navbar-item" href="https://enterprise.spring.io/">Overview</a>
            <a class="navbar-item" href="https://enterprise.spring.io/lts-releases">Long-term Support</a>
            <a class="navbar-item" href="https://enterprise.spring.io/spring-application-advisor">Automated Upgrades</a>
            <a class="navbar-item" href="https://enterprise.spring.io/enterprise-extensions">Governance and Compliance</a>
            <a class="navbar-item" href="https://enterprise.spring.io/enterprise-components">Modern App Development</a>
          </div>
        </div>
      </div>
    </div>
    <label class="theme-toggler">
      <input
        type="checkbox"
        type="checkbox"
        id="switch-theme-checkbox"
        name="switch-theme-checkbox"
      />
      <span class="icon"><svg
          aria-hidden="true"
          focusable="false"
          data-prefix="fas"
          data-icon="moon"
          class="svg-inline--fa fa-moon moon"
          role="img"
          xmlns="http://www.w3.org/2000/svg"
          viewBox="0 0 384 512"
        ><path
            fill="currentColor"
            d="M223.5 32C100 32 0 132.3 0 256S100 480 223.5 480c60.6 0 115.5-24.2 155.8-63.4c5-4.9 6.3-12.5 3.1-18.7s-10.1-9.7-17-8.5c-9.8 1.7-19.8 2.6-30.1 2.6c-96.9 0-175.5-78.8-175.5-176c0-65.8 36-123.1 89.3-153.3c6.1-3.5 9.2-10.5 7.7-17.3s-7.3-11.9-14.3-12.5c-6.3-.5-12.6-.8-19-.8z"
          ></path>
        </svg>
        <svg
          aria-hidden="true"
          focusable="false"
          data-prefix="fas"
          data-icon="sun"
          class="svg-inline--fa fa-sun sun"
          role="img"
          xmlns="http://www.w3.org/2000/svg"
          viewBox="0 0 512 512"
        ><path
            fill="currentColor"
            d="M361.5 1.2c5 2.1 8.6 6.6 9.6 11.9L391 121l107.9 19.8c5.3 1 9.8 4.6 11.9 9.6s1.5 10.7-1.6 15.2L446.9 256l62.3 90.3c3.1 4.5 3.7 10.2 1.6 15.2s-6.6 8.6-11.9 9.6L391 391 371.1 498.9c-1 5.3-4.6 9.8-9.6 11.9s-10.7 1.5-15.2-1.6L256 446.9l-90.3 62.3c-4.5 3.1-10.2 3.7-15.2 1.6s-8.6-6.6-9.6-11.9L121 391 13.1 371.1c-5.3-1-9.8-4.6-11.9-9.6s-1.5-10.7 1.6-15.2L65.1 256 2.8 165.7c-3.1-4.5-3.7-10.2-1.6-15.2s6.6-8.6 11.9-9.6L121 121 140.9 13.1c1-5.3 4.6-9.8 9.6-11.9s10.7-1.5 15.2 1.6L256 65.1 346.3 2.8c4.5-3.1 10.2-3.7 15.2-1.6zM160 256a96 96 0 1 1 192 0 96 96 0 1 1 -192 0zm224 0a128 128 0 1 0 -256 0 128 128 0 1 0 256 0z"
          ></path>
        </svg></span>
      <span class="text">light</span>
    </label>
  </nav>
</header>
<script>
!function (theme) {
  if (theme === 'dark') {
    document.getElementById('switch-theme-checkbox').parentElement.classList.add('active')
  }
}(localStorage && localStorage.getItem('theme') || (matchMedia('(prefers-color-scheme: dark)')?.matches && 'dark'))
</script>
<div class="body">
<div class="nav-container" data-component="security" data-version="7.1.1">
  <aside class="nav">
    <div class="panels">
      <div class="nav-panel-menu is-active" data-panel="menu">
        <nav class="nav-menu">
<div class="context">
  <span class="title">Spring Security</span>
  <span class="version">7.1.1</span>
  <button class="browse-version" id="browse-version">
    <svg
      height="24px"
      id="Layer_1"
      style="enable-background:new 0 0 512 512;"
      version="1.1"
      viewBox="0 0 512 512"
      width="24px"
      xml:space="preserve"
    ><g><path
          d="M256,224c-17.7,0-32,14.3-32,32s14.3,32,32,32c17.7,0,32-14.3,32-32S273.7,224,256,224L256,224z"
        ></path><path
          d="M128.4,224c-17.7,0-32,14.3-32,32s14.3,32,32,32c17.7,0,32-14.3,32-32S146,224,128.4,224L128.4,224z"
        ></path><path
          d="M384,224c-17.7,0-32,14.3-32,32s14.3,32,32,32s32-14.3,32-32S401.7,224,384,224L384,224z"
        ></path></g></svg>
  </button>
  <div class="search">
  <button class="DocSearch-Button search-button">
    <svg enable-background="new 0 0 32 32" id="Glyph" version="1.1" viewBox="0 0 32 32" xml:space="preserve" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink">
      <path d="M27.414,24.586l-5.077-5.077C23.386,17.928,24,16.035,24,14c0-5.514-4.486-10-10-10S4,8.486,4,14  s4.486,10,10,10c2.035,0,3.928-0.614,5.509-1.663l5.077,5.077c0.78,0.781,2.048,0.781,2.828,0  C28.195,26.633,28.195,25.367,27.414,24.586z M7,14c0-3.86,3.14-7,7-7s7,3.14,7,7s-3.14,7-7,7S7,17.86,7,14z" id="XMLID_223_"/>
    </svg>
    <span>Search</span>
    <span class="search-key"></span>
  </button>
</div>
</div><ul class="nav-list">
  <li class="nav-item" data-depth="0">
<ul class="nav-list">
  <li class="nav-item" data-depth="1">
    <a class="nav-link"  href="../../index.html">Overview</a>
  </li>
  <li class="nav-item" data-depth="1">
    <a class="nav-link"  href="../../prerequisites.html">Prerequisites</a>
  </li>
  <li class="nav-item" data-depth="1">
    <a class="nav-link"  href="../../community.html">Community</a>
  </li>
  <li class="nav-item" data-depth="1">
    <a class="nav-link"  href="../../whats-new.html">What&#8217;s New</a>
  </li>
  <li class="nav-item" data-depth="1">
    <a class="nav-link"  href="../../migration-8/index.html">Preparing for 8.0</a>
  </li>
  <li class="nav-item" data-depth="1">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../../migration/index.html">Migrating to 7</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../../migration/servlet/index.html">Servlet</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../migration/servlet/authorization.html">Authorization</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../migration/servlet/oauth2.html">OAuth 2.0</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../migration/servlet/saml2.html">SAML 2.0</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../../migration/reactive.html">Reactive</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="1">
    <a class="nav-link"  href="../../getting-spring-security.html">Getting Spring Security</a>
  </li>
  <li class="nav-item" data-depth="1">
    <a class="nav-link attachment"  href="../../api/java/index.html">Javadoc</a>
  </li>
  <li class="nav-item" data-depth="1">
    <a class="nav-link attachment"  href="../../api/kotlin/index.html">KDoc</a>
  </li>
  <li class="nav-item" data-depth="1">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../../features/index.html">Features</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../../features/authentication/index.html">Authentication</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../features/authentication/password-storage.html">Password Storage</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../../features/authorization/index.html">Authorization</a>
  </li>
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../../features/exploits/index.html">Protection Against Exploits</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../features/exploits/csrf.html">CSRF</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../features/exploits/headers.html">HTTP Headers</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../features/exploits/http.html">HTTP Requests</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../../features/integrations/index.html">Integrations</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <button class="nav-item-toggle"></button>
    <span class="nav-text">REST Client</span>
<ul class="nav-list">
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../features/integrations/rest/http-service-client.html">HTTP Service Clients</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../features/integrations/cryptography.html">Cryptography</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../features/integrations/data.html">Spring Data</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../features/integrations/concurrency.html">Java&#8217;s Concurrency APIs</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../features/integrations/jackson.html">Jackson</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../features/integrations/localization.html">Localization</a>
  </li>
</ul>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="1">
    <a class="nav-link"  href="../../modules.html">Project Modules</a>
  </li>
  <li class="nav-item" data-depth="1">
    <a class="nav-link"  href="../../samples.html">Samples</a>
  </li>
  <li class="nav-item" data-depth="1">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../index.html">Servlet Applications</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../getting-started.html">Getting Started</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../architecture.html">Architecture</a>
  </li>
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="index.html">Authentication</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="architecture.html">Authentication Architecture</a>
  </li>
  <li class="nav-item" data-depth="3">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="passwords/index.html">Username/Password</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="4">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="passwords/input.html">Reading Username/Password</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="passwords/form.html">Form</a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="passwords/basic.html">Basic</a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="passwords/digest.html">Digest</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="4">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="passwords/storage.html">Password Storage</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="passwords/in-memory.html">In Memory</a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="passwords/jdbc.html">JDBC</a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="passwords/user-details.html">UserDetails</a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="passwords/credentials-container.html">CredentialsContainer</a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="passwords/erasure.html">Password Erasure</a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="passwords/user-details-service.html">UserDetailsService</a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="passwords/password-encoder.html">PasswordEncoder</a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="passwords/dao-authentication-provider.html">DaoAuthenticationProvider</a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="passwords/ldap.html">LDAP</a>
  </li>
</ul>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="mfa.html">Multi-Factor Authentication</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="persistence.html">Persistence</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="passkeys.html">Passkeys</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="onetimetoken.html">One-Time Token</a>
  </li>
  <li class="nav-item is-current-page" data-depth="3">
    <a class="nav-link"  href="session-management.html">Session Management</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="rememberme.html">Remember Me</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="anonymous.html">Anonymous</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="preauth.html">Pre-Authentication</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="jaas.html">JAAS</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="cas.html">CAS</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="x509.html">X509</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="runas.html">Run-As</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="logout.html">Logout</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="events.html">Authentication Events</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="kerberos/index.html">Kerberos</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="kerberos/introduction.html">Introduction</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="kerberos/ssk.html">Reference</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="kerberos/samples.html">Samples</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="kerberos/appendix.html">Appendices</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../authorization/index.html">Authorization</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../authorization/architecture.html">Authorization Architecture</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../authorization/authorize-http-requests.html">Authorize HTTP Requests</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../authorization/method-security.html">Method Security</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../authorization/acls.html">Domain Object Security ACLs</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../authorization/events.html">Authorization Events</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../oauth2/index.html">OAuth2</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../oauth2/login/index.html">OAuth2 Log In</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../oauth2/login/core.html">Core Configuration</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../oauth2/login/advanced.html">Advanced Configuration</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../oauth2/login/logout.html">OIDC Logout</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="3">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../oauth2/client/index.html">OAuth2 Client</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../oauth2/client/core.html">Core Interfaces and Classes</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../oauth2/client/authorization-grants.html">OAuth2 Authorization Grants</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../oauth2/client/client-authentication.html">OAuth2 Client Authentication</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../oauth2/client/authorized-clients.html">OAuth2 Authorized Clients</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="3">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../oauth2/resource-server/index.html">OAuth2 Resource Server</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../oauth2/resource-server/jwt.html">JWT</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../oauth2/resource-server/opaque-token.html">Opaque Token</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../oauth2/resource-server/multitenancy.html">Multitenancy</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../oauth2/resource-server/bearer-tokens.html">Bearer Tokens</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../oauth2/resource-server/dpop-tokens.html">DPoP-bound Access Tokens</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../oauth2/resource-server/protected-resource-metadata.html">Protected Resource Metadata</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="3">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../oauth2/authorization-server/index.html">OAuth2 Authorization Server</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../oauth2/authorization-server/getting-started.html">Getting Started</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../oauth2/authorization-server/configuration-model.html">Configuration Model</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../oauth2/authorization-server/core-model-components.html">Core Model / Components</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../oauth2/authorization-server/protocol-endpoints.html">Protocol Endpoints</a>
  </li>
</ul>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../saml2/index.html">SAML2</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../saml2/login/index.html">SAML2 Log In</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../saml2/login/overview.html">SAML2 Log In Overview</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../saml2/login/authentication-requests.html">SAML2 Authentication Requests</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../saml2/login/authentication.html">SAML2 Authentication Responses</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../saml2/logout.html">SAML2 Logout</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../saml2/metadata.html">SAML2 Metadata</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../saml2/saml-extension-migration.html">Migrating from Spring Security SAML Extension</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../exploits/index.html">Protection Against Exploits</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../exploits/csrf.html">Cross Site Request Forgery (CSRF)</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../exploits/headers.html">Security HTTP Response Headers</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../exploits/http.html">HTTP</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../exploits/firewall.html">HttpFirewall</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../integrations/index.html">Integrations</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../integrations/concurrency.html">Concurrency</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../integrations/localization.html">Localization</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../integrations/servlet-api.html">Servlet APIs</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../integrations/data.html">Spring Data</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../integrations/mvc.html">Spring MVC</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../integrations/websocket.html">WebSocket</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../integrations/cors.html">Spring&#8217;s CORS Support</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../integrations/jsp-taglibs.html">JSP Taglib</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../integrations/observability.html">Observability</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <span class="nav-text">Configuration</span>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../configuration/java.html">Java Configuration</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../configuration/kotlin.html">Kotlin Configuration</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../configuration/xml-namespace.html">Namespace Configuration</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../test/index.html">Testing</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../test/method.html">Method Security</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../test/mockmvc/index.html">MockMvc Support</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../test/mockmvc/setup.html">MockMvc Setup</a>
  </li>
  <li class="nav-item" data-depth="3">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../test/mockmvc/request-post-processors.html">Security RequestPostProcessors</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../test/mockmvc/authentication.html">Mocking Users</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../test/mockmvc/csrf.html">Mocking CSRF</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../test/mockmvc/form-login.html">Mocking Form Login</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../test/mockmvc/http-basic.html">Mocking HTTP Basic</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../test/mockmvc/oauth2.html">Mocking OAuth2</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../test/mockmvc/logout.html">Mocking Logout</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../test/mockmvc/request-builders.html">Security RequestBuilders</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../test/mockmvc/result-matchers.html">Security ResultMatchers</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../test/mockmvc/result-handlers.html">Security ResultHandlers</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../appendix/index.html">Appendix</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../appendix/database-schema.html">Database Schemas</a>
  </li>
  <li class="nav-item" data-depth="3">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../appendix/namespace/index.html">XML Namespace</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../appendix/namespace/authentication-manager.html">Authentication Services</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../appendix/namespace/http.html">Web Security</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../appendix/namespace/method-security.html">Method Security</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../appendix/namespace/ldap.html">LDAP Security</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../appendix/namespace/websocket.html">WebSocket Security</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../appendix/proxy-server.html">Proxy Server Configuration</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../appendix/faq.html">FAQ</a>
  </li>
</ul>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="1">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../../reactive/index.html">Reactive Applications</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../../reactive/getting-started.html">Getting Started</a>
  </li>
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../../reactive/authentication/index.html">Authentication</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../reactive/authentication/x509.html">X.509 Authentication</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../reactive/authentication/logout.html">Logout</a>
  </li>
  <li class="nav-item" data-depth="3">
    <button class="nav-item-toggle"></button>
    <span class="nav-text">Session Management</span>
<ul class="nav-list">
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../reactive/authentication/concurrent-sessions-control.html">Concurrent Sessions Control</a>
  </li>
</ul>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <span class="nav-text">Authorization</span>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../reactive/authorization/authorize-http-requests.html">Authorize HTTP Requests</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../reactive/authorization/method.html">EnableReactiveMethodSecurity</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../../reactive/oauth2/index.html">OAuth2</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../../reactive/oauth2/login/index.html">OAuth2 Log In</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../reactive/oauth2/login/core.html">Core Configuration</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../reactive/oauth2/login/advanced.html">Advanced Configuration</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../reactive/oauth2/login/logout.html">OIDC Logout</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="3">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../../reactive/oauth2/client/index.html">OAuth2 Client</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../reactive/oauth2/client/core.html">Core Interfaces and Classes</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../reactive/oauth2/client/authorization-grants.html">OAuth2 Authorization Grants</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../reactive/oauth2/client/client-authentication.html">OAuth2 Client Authentication</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../reactive/oauth2/client/authorized-clients.html">OAuth2 Authorized Clients</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="3">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../../reactive/oauth2/resource-server/index.html">OAuth2 Resource Server</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../reactive/oauth2/resource-server/jwt.html">JWT</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../reactive/oauth2/resource-server/opaque-token.html">Opaque Token</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../reactive/oauth2/resource-server/multitenancy.html">Multitenancy</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../reactive/oauth2/resource-server/bearer-tokens.html">Bearer Tokens</a>
  </li>
</ul>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../../reactive/exploits/index.html">Protection Against Exploits</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../reactive/exploits/csrf.html">CSRF</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../reactive/exploits/headers.html">Headers</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../reactive/exploits/http.html">HTTP Requests</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../reactive/exploits/firewall.html">ServerWebExchangeFirewall</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <span class="nav-text">Integrations</span>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../reactive/integrations/cors.html">CORS</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../reactive/integrations/rsocket.html">RSocket</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../reactive/integrations/observability.html">Observability</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../../reactive/test/index.html">Testing</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../reactive/test/method.html">Testing Method Security</a>
  </li>
  <li class="nav-item" data-depth="3">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../../reactive/test/web/index.html">Testing Web Security</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../reactive/test/web/setup.html">WebTestClient Setup</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../reactive/test/web/authentication.html">Testing Authentication</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../reactive/test/web/csrf.html">Testing CSRF</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../reactive/test/web/oauth2.html">Testing OAuth 2.0</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../reactive/test/web/x509.html">Testing X509</a>
  </li>
</ul>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../../reactive/configuration/webflux.html">WebFlux Security</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="1">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../../native-image/index.html">GraalVM Native Image Support</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../../native-image/method-security.html">Method Security</a>
  </li>
</ul>
  </li>
</ul>
  </li>
</ul>
          <div class="toggle-sm">
            <button id="nav-toggle-2" class="nav-toggle"></button>
          </div>
        </nav>
      </div>
      <div class="nav-collapse">
        <button id="nav-collapse-toggle"><span></span></button>        
      </div>
    </div>
    <div class="nav-resize"></div>
  </aside>
</div>
<script>
!function (sidebar) {
  if (sidebar) {
    document.body.classList.add('nav-sm')
  }
}(localStorage && localStorage.getItem('sidebar') === 'close')
</script><main class="article">
<div class="toolbar" role="navigation">
  <button id="nav-toggle-1" class="nav-toggle"></button>
<div class="search">
  <button class="DocSearch-Button search-button">
    <svg enable-background="new 0 0 32 32" id="Glyph" version="1.1" viewBox="0 0 32 32" xml:space="preserve" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink">
      <path d="M27.414,24.586l-5.077-5.077C23.386,17.928,24,16.035,24,14c0-5.514-4.486-10-10-10S4,8.486,4,14  s4.486,10,10,10c2.035,0,3.928-0.614,5.509-1.663l5.077,5.077c0.78,0.781,2.048,0.781,2.828,0  C28.195,26.633,28.195,25.367,27.414,24.586z M7,14c0-3.86,3.14-7,7-7s7,3.14,7,7s-3.14,7-7,7S7,17.86,7,14z" id="XMLID_223_"/>
    </svg>
    <span>Search</span>
    <span class="search-key"></span>
  </button>
</div>
</div>
  <div class="content">
<aside class="sidebar">
  <div class="content">
    <div
      class="toc"
      data-title="Authentication Persistence and Session Management"
      data-levels="2"
    >
      <div class="toc-menu"></div>
    </div>
    <div class="sidebar-links">
        <a href="https://github.com/spring-projects/spring-security/blob/7.1.1/docs/modules/ROOT/pages/servlet/authentication/session-management.adoc">
          <svg
            xmlns="http://www.w3.org/2000/svg"
            height="24"
            viewBox="0 0 24 24"
            width="24"
          ><path
              d="m16 2.012 3 3L16.713 7.3l-3-3zM4 14v3h3l8.299-8.287-3-3zm0 6h16v2H4z"
            ></path></svg>
          Edit this Page
        </a>
              <a href="https://github.com/spring-projects/spring-security" title="GitHub">
          <svg
            xmlns="http://www.w3.org/2000/svg"
            height="512px"
            id="Layer_1"
            version="1.1"
            viewBox="0 0 512 512"
            width="512px"
          ><style type="text/css"><![CDATA[
              .st0{fill-rule:evenodd;clip-rule:evenodd;} ]]></style><g><path
                class="st0"
                d="M256,32C132.3,32,32,134.8,32,261.7c0,101.5,64.2,187.5,153.2,217.9c11.2,2.1,15.3-5,15.3-11.1   c0-5.5-0.2-19.9-0.3-39.1c-62.3,13.9-75.5-30.8-75.5-30.8c-10.2-26.5-24.9-33.6-24.9-33.6c-20.3-14.3,1.5-14,1.5-14   c22.5,1.6,34.3,23.7,34.3,23.7c20,35.1,52.4,25,65.2,19.1c2-14.8,7.8-25,14.2-30.7c-49.7-5.8-102-25.5-102-113.5   c0-25.1,8.7-45.6,23-61.6c-2.3-5.8-10-29.2,2.2-60.8c0,0,18.8-6.2,61.6,23.5c17.9-5.1,37-7.6,56.1-7.7c19,0.1,38.2,2.6,56.1,7.7   c42.8-29.7,61.5-23.5,61.5-23.5c12.2,31.6,4.5,55,2.2,60.8c14.3,16.1,23,36.6,23,61.6c0,88.2-52.4,107.6-102.3,113.3   c8,7.1,15.2,21.1,15.2,42.5c0,30.7-0.3,55.5-0.3,63c0,6.1,4,13.3,15.4,11C415.9,449.1,480,363.1,480,261.7   C480,134.8,379.7,32,256,32z"
              ></path></g></svg>
          GitHub Project
        </a>
        <a href="https://stackoverflow.com/tags/spring-security">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 384 512"><path
              d="M290.7 311L95 269.7 86.8 309l195.7 41zm51-87L188.2 95.7l-25.5 30.8 153.5 128.3zm-31.2 39.7L129.2 179l-16.7 36.5L293.7 300zM262 32l-32 24 119.3 160.3 32-24zm20.5 328h-200v39.7h200zm39.7 80H42.7V320h-40v160h359.5V320h-40z"
            ></path></svg>
          Stack Overflow
        </a>
    </div>
  </div>
</aside>
<article class="doc">
<div class="breadcrumbs-container">
  <nav class="breadcrumbs" aria-label="breadcrumbs">
    <ul>
      <li id="copy-url" title="Copy versioned URL"></li>
      <li><a href="../../index.html">Spring Security</a></li>
      <li><a href="../index.html">Servlet Applications</a></li>
      <li><a href="index.html">Authentication</a></li>
      <li><a href="session-management.html">Session Management</a></li>
    </ul>
  </nav>
</div><h1 id="page-title" class="page">Authentication Persistence and Session Management</h1>
<div id="preamble">
<div class="sectionbody">
<div class="paragraph">
<p>Once you have got an application that is <a href="index.html" class="xref page">authenticating requests</a>, it is important to consider how that resulting authentication will be persisted and restored on future requests.</p>
</div>
<div class="paragraph">
<p>This is done automatically by default.
If you have a custom filter or controller that is setting the security context, you will need to use a <code>SecurityContextRepository</code> to persist it across requests.</p>
</div>
<div class="paragraph">
<p>If you are upgrading from an older version, you may be interested in the <code>requireExplicitSave</code> setting that preserves Spring Security 5&#8217;s default, though note that this is primarily for migration purposes.</p>
</div>
<div class="paragraph">
<p>If you like, <a href="#how-it-works-requireexplicitsave">you can read more about what requireExplicitSave is doing</a> or <a href="#requireexplicitsave">why it&#8217;s important</a>. Otherwise, in most cases you are done with this section.</p>
</div>
<div class="paragraph">
<p>But before you leave, consider if any of these use cases fit your application:</p>
</div>
<div class="ulist">
<ul>
<li>
<p>I want to <a href="#understanding-session-management-components">Understand Session Management&#8217;s components</a></p>
</li>
<li>
<p>I want to <a href="#ns-concurrent-sessions">restrict the number of times</a> a user can be logged in concurrently</p>
</li>
<li>
<p>I want <a href="#store-authentication-manually">to store the authentication directly</a> myself instead of Spring Security doing it for me</p>
</li>
<li>
<p>I am storing the authentication manually and I want <a href="#properly-clearing-authentication">to remove it</a></p>
</li>
<li>
<p>I am using <a href="#the-sessionmanagementfilter"><code>SessionManagementFilter</code></a> and I need <a href="#moving-away-from-sessionmanagementfilter">guidance on moving away from that</a></p>
</li>
<li>
<p>I want to store the authentication <a href="#customizing-where-authentication-is-stored">in something other than the session</a></p>
</li>
<li>
<p>I am using a <a href="#stateless-authentication">stateless authentication</a>, but <a href="#storing-stateless-authentication-in-the-session">I&#8217;d still like to store it in the session</a></p>
</li>
<li>
<p>I am using <code>SessionCreationPolicy.NEVER</code> but <a href="#never-policy-session-still-created">the application is still creating sessions</a>.</p>
</li>
</ul>
</div>
</div>
</div>
<div class="sect1">
<h2 id="understanding-session-management-components"><a class="anchor" href="#understanding-session-management-components"></a>Understanding Session Management&#8217;s Components</h2>
<div class="sectionbody">
<div class="paragraph">
<p>The Session Management support is composed of a few components that work together to provide the functionality.
Those components are, <a href="persistence.html#securitycontextholderfilter" class="xref page">the <code>SecurityContextHolderFilter</code></a>, <a href="persistence.html#securitycontextpersistencefilter" class="xref page">the <code>SecurityContextPersistenceFilter</code></a> and <a href="#the-sessionmanagementfilter">the <code>SessionManagementFilter</code></a>.</p>
</div>
<div class="admonitionblock note">
<table>
<tr>
<td class="icon">
<i class="fa icon-note" title="Note"></i>
</td>
<td class="content">
<div class="paragraph">
<p>In Spring Security 6, the <code>SecurityContextPersistenceFilter</code> and <code>SessionManagementFilter</code> are not set by default.
In addition to that, any application should only have either <code>SecurityContextHolderFilter</code> or <code>SecurityContextPersistenceFilter</code> set, never both.</p>
</div>
</td>
</tr>
</table>
</div>
<div class="sect2">
<h3 id="the-sessionmanagementfilter"><a class="anchor" href="#the-sessionmanagementfilter"></a>The <code>SessionManagementFilter</code></h3>
<div class="paragraph">
<p>The <code>SessionManagementFilter</code> checks the contents of the <code>SecurityContextRepository</code> against the current contents of the <code>SecurityContextHolder</code> to determine whether a user has been authenticated during the current request, typically by a non-interactive authentication mechanism, such as pre-authentication or remember-me  <sup class="footnote">[<a id="_footnoteref_1" class="footnote" href="#_footnotedef_1" title="View footnote.">1</a>]</sup>.
If the repository contains a security context, the filter does nothing.
If it doesn&#8217;t, and the thread-local <code>SecurityContext</code> contains a (non-anonymous) <code>Authentication</code> object, the filter assumes they have been authenticated by a previous filter in the stack.
It will then invoke the configured <code>SessionAuthenticationStrategy</code>.</p>
</div>
<div class="paragraph">
<p>If the user is not currently authenticated, the filter will check whether an invalid session ID has been requested (because of a timeout, for example) and will invoke the configured <code>InvalidSessionStrategy</code>, if one is set.
The most common behaviour is just to redirect to a fixed URL and this is encapsulated in the standard implementation <code>SimpleRedirectInvalidSessionStrategy</code>.
The latter is also used when configuring an invalid session URL through the namespace, <a href="#session-mgmt">as described earlier</a>.</p>
</div>
<div class="sect3">
<h4 id="moving-away-from-sessionmanagementfilter"><a class="anchor" href="#moving-away-from-sessionmanagementfilter"></a>Moving Away From <code>SessionManagementFilter</code></h4>
<div class="paragraph">
<p>In Spring Security 5, the default configuration relies on <code>SessionManagementFilter</code> to detect if a user just authenticated and invoke the <a href="../../api/java/org/springframework/security/web/authentication/session/SessionAuthenticationStrategy.html" class="xref attachment apiref">SessionAuthenticationStrategy</a>.
The problem with this is that it means that in a typical setup, the <code>HttpSession</code> must be read for every request.</p>
</div>
<div class="paragraph">
<p>In Spring Security 6, the default is that authentication mechanisms themselves must invoke the <code>SessionAuthenticationStrategy</code>.
This means that there is no need to detect when <code>Authentication</code> is done and thus the <code>HttpSession</code> does not need to be read for every request.</p>
</div>
</div>
<div class="sect3">
<h4 id="_things_to_consider_when_moving_away_from_sessionmanagementfilter"><a class="anchor" href="#_things_to_consider_when_moving_away_from_sessionmanagementfilter"></a>Things To Consider When Moving Away From <code>SessionManagementFilter</code></h4>
<div class="paragraph">
<p>In Spring Security 6, the <code>SessionManagementFilter</code> is not used by default, therefore, some methods from the <code>sessionManagement</code> DSL will not have any effect.</p>
</div>
<table class="tableblock frame-all grid-all stretch">
<colgroup>
<col style="width: 50%;">
<col style="width: 50%;">
</colgroup>
<thead>
<tr>
<th class="tableblock halign-left valign-top">Method</th>
<th class="tableblock halign-left valign-top">Replacement</th>
</tr>
</thead>
<tbody>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>sessionAuthenticationErrorUrl</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock">Configure an <a href="../../api/java/org/springframework/security/web/authentication/AuthenticationFailureHandler.html" class="xref attachment apiref"><code>AuthenticationFailureHandler</code></a> in your authentication mechanism</p></td>
</tr>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>sessionAuthenticationFailureHandler</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock">Configure an <a href="../../api/java/org/springframework/security/web/authentication/AuthenticationFailureHandler.html" class="xref attachment apiref"><code>AuthenticationFailureHandler</code></a> in your authentication mechanism</p></td>
</tr>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>sessionAuthenticationStrategy</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock">Configure an <code>SessionAuthenticationStrategy</code> in your authentication mechanism as <a href="#moving-away-from-sessionmanagementfilter">discussed above</a></p></td>
</tr>
</tbody>
</table>
<div class="paragraph">
<p>If you try to use any of these methods, an exception will be thrown.</p>
</div>
</div>
</div>
</div>
</div>
<div class="sect1">
<h2 id="customizing-where-authentication-is-stored"><a class="anchor" href="#customizing-where-authentication-is-stored"></a>Customizing Where the Authentication Is Stored</h2>
<div class="sectionbody">
<div class="paragraph">
<p>By default, Spring Security stores the security context for you in the HTTP session. However, here are several reasons you may want to customize that:</p>
</div>
<div class="ulist">
<ul>
<li>
<p>You may want to call individual setters on the <code>HttpSessionSecurityContextRepository</code> instance</p>
</li>
<li>
<p>You may want to store the security context in a cache or database to enable horizontal scaling</p>
</li>
</ul>
</div>
<div class="paragraph">
<p>First, you need to create an implementation of <code>SecurityContextRepository</code> or use an existing implementation like <code>HttpSessionSecurityContextRepository</code>, then you can set it in <code>HttpSecurity</code>.</p>
</div>
<div id="customizing-the-securitycontextrepository" class="openblock tabs is-sync is-loading">
<div class="title">Customizing the <code>SecurityContextRepository</code></div>
<div class="content">
<div class="ulist tablist">
<ul>
<li id="customizing_the_securitycontextrepository_java" class="tab">
<p>Java</p>
</li>
<li id="customizing_the_securitycontextrepository_kotlin" class="tab">
<p>Kotlin</p>
</li>
<li id="customizing_the_securitycontextrepository_xml" class="tab">
<p>XML</p>
</li>
</ul>
</div>
<div id="customizing_the_securitycontextrepository_java--panel" class="tabpanel" aria-labelledby="customizing_the_securitycontextrepository_java">
<div class="listingblock primary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Bean
public SecurityFilterChain filterChain(HttpSecurity http) {
    SecurityContextRepository repo = new MyCustomSecurityContextRepository();
    http
        // ...
        .securityContext((context) -&gt; context
            .securityContextRepository(repo)
        );
    return http.build();
}</code></pre>
</div>
</div>
</div>
<div id="customizing_the_securitycontextrepository_kotlin--panel" class="tabpanel" aria-labelledby="customizing_the_securitycontextrepository_kotlin">
<div class="listingblock secondary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-kotlin hljs" data-lang="kotlin">@Bean
open fun filterChain(http: HttpSecurity): SecurityFilterChain {
    val repo = MyCustomSecurityContextRepository()
    http {
        // ...
        securityContext {
            securityContextRepository = repo
        }
    }
    return http.build()
}</code></pre>
</div>
</div>
</div>
<div id="customizing_the_securitycontextrepository_xml--panel" class="tabpanel" aria-labelledby="customizing_the_securitycontextrepository_xml">
<div class="listingblock secondary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-xml hljs" data-lang="xml">&lt;http security-context-repository-ref="repo"&gt;
    &lt;!-- ... --&gt;
&lt;/http&gt;
&lt;bean name="repo" class="com.example.MyCustomSecurityContextRepository" /&gt;</code></pre>
</div>
</div>
</div>
</div>
</div>
<div class="admonitionblock note">
<table>
<tr>
<td class="icon">
<i class="fa icon-note" title="Note"></i>
</td>
<td class="content">
<div class="paragraph">
<p>The above configuration sets the <code>SecurityContextRepository</code> on the <code>SecurityContextHolderFilter</code> and <strong>participating</strong> authentication filters, like <code>UsernamePasswordAuthenticationFilter</code>.
To also set it in stateless filters, please see <a href="#storing-stateless-authentication-in-the-session">how to customize the <code>SecurityContextRepository</code> for Stateless Authentication</a>.</p>
</div>
</td>
</tr>
</table>
</div>
<div class="paragraph">
<p>If you are using a custom authentication mechanism, you might want to <a href="#store-authentication-manually">store the <code>Authentication</code> by yourself</a>.</p>
</div>
<div class="sect2">
<h3 id="store-authentication-manually"><a class="anchor" href="#store-authentication-manually"></a>Storing the <code>Authentication</code> manually</h3>
<div class="paragraph">
<p>In some cases, for example, you might be authenticating a user manually instead of relying on Spring Security filters.
You can use a custom filters or a <a href="https://docs.spring.io/spring-framework/reference/7.0.9//web.html#mvc-controller">Spring MVC controller</a> endpoint to do that.
If you want to save the authentication between requests, in the <code>HttpSession</code>, for example, you have to do so:</p>
</div>
<div id="_tabs_2" class="openblock tabs is-sync is-loading">
<div class="content">
<div class="ulist tablist">
<ul>
<li id="_tabs_2_java" class="tab">
<p>Java</p>
</li>
</ul>
</div>
<div id="_tabs_2_java--panel" class="tabpanel" aria-labelledby="_tabs_2_java">
<div class="listingblock primary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">private SecurityContextRepository securityContextRepository =
        new HttpSessionSecurityContextRepository(); <i class="conum" data-value="1"></i><b>(1)</b>

@PostMapping("/login")
public void login(@RequestBody LoginRequest loginRequest, HttpServletRequest request, HttpServletResponse response) { <i class="conum" data-value="2"></i><b>(2)</b>
    UsernamePasswordAuthenticationToken token = UsernamePasswordAuthenticationToken.unauthenticated(
        loginRequest.getUsername(), loginRequest.getPassword()); <i class="conum" data-value="3"></i><b>(3)</b>
    Authentication authentication = authenticationManager.authenticate(token); <i class="conum" data-value="4"></i><b>(4)</b>
    SecurityContext context = securityContextHolderStrategy.createEmptyContext();
    context.setAuthentication(authentication); <i class="conum" data-value="5"></i><b>(5)</b>
    securityContextHolderStrategy.setContext(context);
    securityContextRepository.saveContext(context, request, response); <i class="conum" data-value="6"></i><b>(6)</b>
}

class LoginRequest {

    private String username;
    private String password;

    // getters and setters
}</code></pre>
</div>
</div>
</div>
</div>
</div>
<div class="colist arabic">
<table>
<tr>
<td><i class="conum" data-value="1"></i><b>1</b></td>
<td>Add the <code>SecurityContextRepository</code> to the controller</td>
</tr>
<tr>
<td><i class="conum" data-value="2"></i><b>2</b></td>
<td>Inject the <code>HttpServletRequest</code> and <code>HttpServletResponse</code> to be able to save the <code>SecurityContext</code></td>
</tr>
<tr>
<td><i class="conum" data-value="3"></i><b>3</b></td>
<td>Create an unauthenticated <code>UsernamePasswordAuthenticationToken</code> using the provided credentials</td>
</tr>
<tr>
<td><i class="conum" data-value="4"></i><b>4</b></td>
<td>Call <code>AuthenticationManager#authenticate</code> to authenticate the user</td>
</tr>
<tr>
<td><i class="conum" data-value="5"></i><b>5</b></td>
<td>Create a <code>SecurityContext</code> and set the <code>Authentication</code> in it</td>
</tr>
<tr>
<td><i class="conum" data-value="6"></i><b>6</b></td>
<td>Save the <code>SecurityContext</code> in the <code>SecurityContextRepository</code></td>
</tr>
</table>
</div>
<div class="paragraph">
<p>And that&#8217;s it.
If you are not sure what <code>securityContextHolderStrategy</code> is in the above example, you can read more about it in the <a href="#use-securitycontextholderstrategy">Using <code>SecurityContextStrategy</code> section</a>.</p>
</div>
</div>
<div class="sect2">
<h3 id="properly-clearing-authentication"><a class="anchor" href="#properly-clearing-authentication"></a>Properly Clearing an Authentication</h3>
<div class="paragraph">
<p>If you are using Spring Security&#8217;s <a href="logout.html" class="xref page">Logout Support</a> then it handles a lot of stuff for you including clearing and saving the context.
But, let&#8217;s say you need to manually log users out of your app. In that case, you&#8217;ll need to make sure you&#8217;re <a href="logout.html#creating-custom-logout-endpoint" class="xref page">clearing and saving the context properly</a>.</p>
</div>
</div>
<div class="sect2">
<h3 id="stateless-authentication"><a class="anchor" href="#stateless-authentication"></a>Configuring Persistence for Stateless Authentication</h3>
<div class="paragraph">
<p>Sometimes there is no need to create and maintain a <code>HttpSession</code> for example, to persist the authentication across requests.
Some authentication mechanisms like <a href="passwords/basic.html" class="xref page">HTTP Basic</a> are stateless and, therefore, re-authenticates the user on every request.</p>
</div>
<div class="paragraph">
<p>If you do not wish to create sessions, you can use <code>SessionCreationPolicy.STATELESS</code>, like so:</p>
</div>
<div id="_tabs_3" class="openblock tabs is-sync is-loading">
<div class="content">
<div class="ulist tablist">
<ul>
<li id="_tabs_3_java" class="tab">
<p>Java</p>
</li>
<li id="_tabs_3_kotlin" class="tab">
<p>Kotlin</p>
</li>
<li id="_tabs_3_xml" class="tab">
<p>XML</p>
</li>
</ul>
</div>
<div id="_tabs_3_java--panel" class="tabpanel" aria-labelledby="_tabs_3_java">
<div class="listingblock primary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Bean
public SecurityFilterChain filterChain(HttpSecurity http) {
    http
        // ...
        .sessionManagement((session) -&gt; session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );
    return http.build();
}</code></pre>
</div>
</div>
</div>
<div id="_tabs_3_kotlin--panel" class="tabpanel" aria-labelledby="_tabs_3_kotlin">
<div class="listingblock secondary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-kotlin hljs" data-lang="kotlin">@Bean
open fun filterChain(http: HttpSecurity): SecurityFilterChain {
    http {
        // ...
        sessionManagement {
            sessionCreationPolicy = SessionCreationPolicy.STATELESS
        }
    }
    return http.build()
}</code></pre>
</div>
</div>
</div>
<div id="_tabs_3_xml--panel" class="tabpanel" aria-labelledby="_tabs_3_xml">
<div class="listingblock secondary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-xml hljs" data-lang="xml">&lt;http create-session="stateless"&gt;
    &lt;!-- ... --&gt;
&lt;/http&gt;</code></pre>
</div>
</div>
</div>
</div>
</div>
<div class="paragraph">
<p>The above configuration is <a href="#customizing-where-authentication-is-stored">configuring the <code>SecurityContextRepository</code></a> to use a <code>NullSecurityContextRepository</code> and is also <a href="../architecture.html#requestcache-prevent-saved-request" class="xref page">preventing the request from being saved in the session</a>.</p>
</div>
<div id="never-policy-session-still-created" class="paragraph">
<p>If you are using <code>SessionCreationPolicy.NEVER</code>, you might notice that the application is still creating a <code>HttpSession</code>.
In most cases, this happens because the <a href="../architecture.html#savedrequests" class="xref page">request is saved in the session</a> for the authenticated resource to re-request after authentication is successful.
To avoid that, please refer to <a href="../architecture.html#requestcache-prevent-saved-request" class="xref page">how to prevent the request of being saved</a> section.</p>
</div>
<div class="sect3">
<h4 id="storing-stateless-authentication-in-the-session"><a class="anchor" href="#storing-stateless-authentication-in-the-session"></a>Storing Stateless Authentication in the Session</h4>
<div class="paragraph">
<p>If, for some reason, you are using a stateless authentication mechanism, but you still want to store the authentication in the session you can use the <code>HttpSessionSecurityContextRepository</code> instead of the <code>NullSecurityContextRepository</code>.</p>
</div>
<div class="paragraph">
<p>For the <a href="passwords/basic.html" class="xref page">HTTP Basic</a>, you can add <a href="../configuration/java.html#post-processing-configured-objects" class="xref page">a <code>ObjectPostProcessor</code></a> that changes the <code>SecurityContextRepository</code> used by the <code>BasicAuthenticationFilter</code>:</p>
</div>
<div id="_tabs_4" class="openblock tabs is-sync is-loading">
<div class="title">Store HTTP Basic authentication in the <code>HttpSession</code></div>
<div class="content">
<div class="ulist tablist">
<ul>
<li id="_tabs_4_java" class="tab">
<p>Java</p>
</li>
</ul>
</div>
<div id="_tabs_4_java--panel" class="tabpanel" aria-labelledby="_tabs_4_java">
<div class="listingblock primary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Bean
SecurityFilterChain web(HttpSecurity http) throws Exception {
    http
        // ...
        .httpBasic((basic) -&gt; basic
            .addObjectPostProcessor(new ObjectPostProcessor&lt;BasicAuthenticationFilter&gt;() {
                @Override
                public &lt;O extends BasicAuthenticationFilter&gt; O postProcess(O filter) {
                    filter.setSecurityContextRepository(new HttpSessionSecurityContextRepository());
                    return filter;
                }
            })
        );

    return http.build();
}</code></pre>
</div>
</div>
</div>
</div>
</div>
<div class="paragraph">
<p>The above also applies to others authentication mechanisms, like <a href="../oauth2/resource-server/index.html" class="xref page">Bearer Token Authentication</a>.</p>
</div>
</div>
</div>
</div>
</div>
<div class="sect1">
<h2 id="requireexplicitsave"><a class="anchor" href="#requireexplicitsave"></a>Understanding Require Explicit Save</h2>
<div class="sectionbody">
<div class="paragraph">
<p>In Spring Security 5, the default behavior is for the <a href="architecture.html#servlet-authentication-securitycontext" class="xref page"><code>SecurityContext</code></a> to automatically be saved to the <a href="persistence.html#securitycontextrepository" class="xref page"><code>SecurityContextRepository</code></a> using the <a href="#securitycontextpersistencefilter"><code>SecurityContextPersistenceFilter</code></a>.
Saving must be done just prior to the <code>HttpServletResponse</code> being committed and just before <code>SecurityContextPersistenceFilter</code>.
Unfortunately, automatic persistence of the <code>SecurityContext</code> can surprise users when it is done prior to the request completing (i.e. just prior to committing the <code>HttpServletResponse</code>).
It also is complex to keep track of the state to determine if a save is necessary causing unnecessary writes to the <code>SecurityContextRepository</code> (i.e. <code>HttpSession</code>) at times.</p>
</div>
<div class="paragraph">
<p>For these reasons, the <code>SecurityContextPersistenceFilter</code> has been deprecated to be replaced with the <code>SecurityContextHolderFilter</code>.
In Spring Security 6, the default behavior is that <a href="persistence.html#securitycontextholderfilter" class="xref page">the <code>SecurityContextHolderFilter</code></a> will only read the <code>SecurityContext</code> from  <code>SecurityContextRepository</code> and populate it in the <code>SecurityContextHolder</code>.
Users now must explicitly save the <code>SecurityContext</code> with the <code>SecurityContextRepository</code> if they want the <code>SecurityContext</code> to persist between requests.
This removes ambiguity and improves performance by only requiring writing to the <code>SecurityContextRepository</code> (i.e. <code>HttpSession</code>) when it is necessary.</p>
</div>
<div class="sect2">
<h3 id="how-it-works-requireexplicitsave"><a class="anchor" href="#how-it-works-requireexplicitsave"></a>How it works</h3>
<div class="paragraph">
<p>In summary, when <code>requireExplicitSave</code> is <code>true</code>, Spring Security sets up <a href="persistence.html#securitycontextholderfilter" class="xref page">the <code>SecurityContextHolderFilter</code></a> instead of <a href="persistence.html#securitycontextpersistencefilter" class="xref page">the <code>SecurityContextPersistenceFilter</code></a></p>
</div>
</div>
</div>
</div>
<div class="sect1">
<h2 id="ns-concurrent-sessions"><a class="anchor" href="#ns-concurrent-sessions"></a>Configuring Concurrent Session Control</h2>
<div class="sectionbody">
<div class="paragraph">
<p>If you wish to place constraints on a single user&#8217;s ability to log in to your application, Spring Security supports this out of the box with the following simple additions.
First, you need to add the following listener to your configuration to keep Spring Security updated about session lifecycle events:</p>
</div>
<div id="_tabs_5" class="openblock tabs is-sync is-loading">
<div class="content">
<div class="ulist tablist">
<ul>
<li id="_tabs_5_java" class="tab">
<p>Java</p>
</li>
<li id="_tabs_5_kotlin" class="tab">
<p>Kotlin</p>
</li>
<li id="_tabs_5_web_xml" class="tab">
<p>web.xml</p>
</li>
</ul>
</div>
<div id="_tabs_5_java--panel" class="tabpanel" aria-labelledby="_tabs_5_java">
<div class="listingblock primary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Bean
public HttpSessionEventPublisher httpSessionEventPublisher() {
    return new HttpSessionEventPublisher();
}</code></pre>
</div>
</div>
</div>
<div id="_tabs_5_kotlin--panel" class="tabpanel" aria-labelledby="_tabs_5_kotlin">
<div class="listingblock secondary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-kotlin hljs" data-lang="kotlin">@Bean
open fun httpSessionEventPublisher(): HttpSessionEventPublisher {
    return HttpSessionEventPublisher()
}</code></pre>
</div>
</div>
</div>
<div id="_tabs_5_web_xml--panel" class="tabpanel" aria-labelledby="_tabs_5_web_xml">
<div class="listingblock secondary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-xml hljs" data-lang="xml">&lt;listener&gt;
&lt;listener-class&gt;
    org.springframework.security.web.session.HttpSessionEventPublisher
&lt;/listener-class&gt;
&lt;/listener&gt;</code></pre>
</div>
</div>
</div>
</div>
</div>
<div class="paragraph">
<p>Then add the following lines to your security configuration:</p>
</div>
<div id="_tabs_6" class="openblock tabs is-sync is-loading">
<div class="content">
<div class="ulist tablist">
<ul>
<li id="_tabs_6_java" class="tab">
<p>Java</p>
</li>
<li id="_tabs_6_kotlin" class="tab">
<p>Kotlin</p>
</li>
<li id="_tabs_6_xml" class="tab">
<p>XML</p>
</li>
</ul>
</div>
<div id="_tabs_6_java--panel" class="tabpanel" aria-labelledby="_tabs_6_java">
<div class="listingblock primary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Bean
public SecurityFilterChain filterChain(HttpSecurity http) {
    http
        .sessionManagement((session) -&gt; session
            .maximumSessions(1)
        );
    return http.build();
}</code></pre>
</div>
</div>
</div>
<div id="_tabs_6_kotlin--panel" class="tabpanel" aria-labelledby="_tabs_6_kotlin">
<div class="listingblock secondary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-kotlin hljs" data-lang="kotlin">@Bean
open fun filterChain(http: HttpSecurity): SecurityFilterChain {
    http {
        sessionManagement {
            sessionConcurrency {
                maximumSessions = 1
            }
        }
    }
    return http.build()
}</code></pre>
</div>
</div>
</div>
<div id="_tabs_6_xml--panel" class="tabpanel" aria-labelledby="_tabs_6_xml">
<div class="listingblock secondary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-xml hljs" data-lang="xml">&lt;http&gt;
...
&lt;session-management&gt;
    &lt;concurrency-control max-sessions="1" /&gt;
&lt;/session-management&gt;
&lt;/http&gt;</code></pre>
</div>
</div>
</div>
</div>
</div>
<div class="paragraph">
<p>This will prevent a user from logging in multiple times - a second login will cause the first to be invalidated.</p>
</div>
<div class="paragraph">
<p>You can also adjust this based on who the user is.
For example, administrators may be able to have more than one session:</p>
</div>
<div id="_tabs_7" class="openblock tabs is-sync is-loading">
<div class="content">
<div class="ulist tablist">
<ul>
<li id="_tabs_7_java" class="tab">
<p>Java</p>
</li>
<li id="_tabs_7_kotlin" class="tab">
<p>Kotlin</p>
</li>
<li id="_tabs_7_xml" class="tab">
<p>XML</p>
</li>
</ul>
</div>
<div id="_tabs_7_java--panel" class="tabpanel" aria-labelledby="_tabs_7_java">
<div class="listingblock primary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Bean
public SecurityFilterChain filterChain(HttpSecurity http) {
	AuthorizationManager&lt;?&gt; isAdmin = AuthorityAuthorizationManager.hasRole("ADMIN");
    http
        .sessionManagement((session) -&gt; session
            .maximumSessions((authentication) -&gt; isAdmin.authorize(() -&gt; authentication, null).isGranted() ? -1 : 1)
        );
    return http.build();
}</code></pre>
</div>
</div>
</div>
<div id="_tabs_7_kotlin--panel" class="tabpanel" aria-labelledby="_tabs_7_kotlin">
<div class="listingblock secondary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-kotlin hljs" data-lang="kotlin">@Bean
open fun filterChain(http: HttpSecurity): SecurityFilterChain {
    val isAdmin: AuthorizationManager&lt;*&gt; = AuthorityAuthorizationManager.hasRole("ADMIN")
    http {
        sessionManagement {
            sessionConcurrency {
                maximumSessions {
                    authentication -&gt; if (isAdmin.authorize({ authentication }, null)!!.isGranted) -1 else 1
                }
            }
        }
    }
    return http.build()
}</code></pre>
</div>
</div>
</div>
<div id="_tabs_7_xml--panel" class="tabpanel" aria-labelledby="_tabs_7_xml">
<div class="listingblock secondary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-xml hljs" data-lang="xml">&lt;http&gt;
...
&lt;session-management&gt;
    &lt;concurrency-control max-sessions-ref="sessionLimit" /&gt;
&lt;/session-management&gt;
&lt;/http&gt;

&lt;b:bean id="sessionLimit" class="my.SessionLimitImplementation"/&gt;</code></pre>
</div>
</div>
</div>
</div>
</div>
<div class="paragraph">
<p>Using Spring Boot, you can test the above configurations in the following way:</p>
</div>
<div id="_tabs_8" class="openblock tabs is-sync is-loading">
<div class="content">
<div class="ulist tablist">
<ul>
<li id="_tabs_8_java" class="tab">
<p>Java</p>
</li>
</ul>
</div>
<div id="_tabs_8_java--panel" class="tabpanel" aria-labelledby="_tabs_8_java">
<div class="listingblock primary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class MaximumSessionsTests {

    @Autowired
    private MockMvc mvc;

    @Test
    void loginOnSecondLoginThenFirstSessionTerminated() throws Exception {
        MvcResult mvcResult = this.mvc.perform(formLogin())
                .andExpect(authenticated())
                .andReturn();

        MockHttpSession firstLoginSession = (MockHttpSession) mvcResult.getRequest().getSession();

        this.mvc.perform(get("/").session(firstLoginSession))
                .andExpect(authenticated());

        this.mvc.perform(formLogin()).andExpect(authenticated());

        // first session is terminated by second login
        this.mvc.perform(get("/").session(firstLoginSession))
                .andExpect(unauthenticated());
    }

}</code></pre>
</div>
</div>
</div>
</div>
</div>
<div class="paragraph">
<p>You can try it using the <a href="https://github.com/spring-projects/spring-security-samples/tree/main/servlet/spring-boot/java/session-management/maximum-sessions">Maximum Sessions sample</a>.</p>
</div>
<div class="paragraph">
<p>It is also common that you would prefer to prevent a second login, in which case you can use:</p>
</div>
<div id="_tabs_9" class="openblock tabs is-sync is-loading">
<div class="content">
<div class="ulist tablist">
<ul>
<li id="_tabs_9_java" class="tab">
<p>Java</p>
</li>
<li id="_tabs_9_kotlin" class="tab">
<p>Kotlin</p>
</li>
<li id="_tabs_9_xml" class="tab">
<p>XML</p>
</li>
</ul>
</div>
<div id="_tabs_9_java--panel" class="tabpanel" aria-labelledby="_tabs_9_java">
<div class="listingblock primary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Bean
public SecurityFilterChain filterChain(HttpSecurity http) {
    http
        .sessionManagement((session) -&gt; session
            .maximumSessions(1)
            .maxSessionsPreventsLogin(true)
        );
    return http.build();
}</code></pre>
</div>
</div>
</div>
<div id="_tabs_9_kotlin--panel" class="tabpanel" aria-labelledby="_tabs_9_kotlin">
<div class="listingblock secondary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-kotlin hljs" data-lang="kotlin">@Bean
open fun filterChain(http: HttpSecurity): SecurityFilterChain {
    http {
        sessionManagement {
            sessionConcurrency {
                maximumSessions = 1
                maxSessionsPreventsLogin = true
            }
        }
    }
    return http.build()
}</code></pre>
</div>
</div>
</div>
<div id="_tabs_9_xml--panel" class="tabpanel" aria-labelledby="_tabs_9_xml">
<div class="listingblock secondary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-xml hljs" data-lang="xml">&lt;http&gt;
&lt;session-management&gt;
    &lt;concurrency-control max-sessions="1" error-if-maximum-exceeded="true" /&gt;
&lt;/session-management&gt;
&lt;/http&gt;</code></pre>
</div>
</div>
</div>
</div>
</div>
<div class="paragraph">
<p>The second login will then be rejected.
By "rejected", we mean that the user will be sent to the <code>authentication-failure-url</code> if form-based login is being used.
If the second authentication takes place through another non-interactive mechanism, such as "remember-me", an "unauthorized" (401) error will be sent to the client.
If instead you want to use an error page, you can add the attribute <code>session-authentication-error-url</code> to the <code>session-management</code> element.</p>
</div>
<div class="paragraph">
<p>Using Spring Boot, you can test the above configuration the following way:</p>
</div>
<div id="_tabs_10" class="openblock tabs is-sync is-loading">
<div class="content">
<div class="ulist tablist">
<ul>
<li id="_tabs_10_java" class="tab">
<p>Java</p>
</li>
</ul>
</div>
<div id="_tabs_10_java--panel" class="tabpanel" aria-labelledby="_tabs_10_java">
<div class="listingblock primary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class MaximumSessionsPreventLoginTests {

    @Autowired
    private MockMvc mvc;

    @Test
    void loginOnSecondLoginThenPreventLogin() throws Exception {
        MvcResult mvcResult = this.mvc.perform(formLogin())
                .andExpect(authenticated())
                .andReturn();

        MockHttpSession firstLoginSession = (MockHttpSession) mvcResult.getRequest().getSession();

        this.mvc.perform(get("/").session(firstLoginSession))
                .andExpect(authenticated());

        // second login is prevented
        this.mvc.perform(formLogin()).andExpect(unauthenticated());

        // first session is still valid
        this.mvc.perform(get("/").session(firstLoginSession))
                .andExpect(authenticated());
    }

}</code></pre>
</div>
</div>
</div>
</div>
</div>
<div class="paragraph">
<p>If you are using a customized authentication filter for form-based login, then you have to configure concurrent session control support explicitly.
You can try it using the <a href="https://github.com/spring-projects/spring-security-samples/tree/main/servlet/spring-boot/java/session-management/maximum-sessions-prevent-login">Maximum Sessions Prevent Login sample</a>.</p>
</div>
<div class="admonitionblock note">
<table>
<tr>
<td class="icon">
<i class="fa icon-note" title="Note"></i>
</td>
<td class="content">
<div class="paragraph">
<p>If you are using a custom implementation of <code>UserDetails</code>, ensure you override the <strong>equals()</strong> and <strong>hashCode()</strong> methods.
The default <code>SessionRegistry</code> implementation in Spring Security relies on an in-memory Map that uses these methods to correctly identify and manage user sessions.
Failing to override them may lead to issues where session tracking and user comparison behave unexpectedly.</p>
</div>
</td>
</tr>
</table>
</div>
</div>
</div>
<div class="sect1">
<h2 id="_detecting_timeouts"><a class="anchor" href="#_detecting_timeouts"></a>Detecting Timeouts</h2>
<div class="sectionbody">
<div class="paragraph">
<p>Sessions expire on their own, and there is nothing that needs to be done to ensure that a security context gets removed.
That said, Spring Security can detect when a session has expired and take specific actions that you indicate.
For example, you may want to redirect to a specific endpoint when a user makes a request with an already-expired session.
This is achieved through the <code>invalidSessionUrl</code> in <code>HttpSecurity</code>:</p>
</div>
<div id="_tabs_11" class="openblock tabs is-sync is-loading">
<div class="content">
<div class="ulist tablist">
<ul>
<li id="_tabs_11_java" class="tab">
<p>Java</p>
</li>
<li id="_tabs_11_kotlin" class="tab">
<p>Kotlin</p>
</li>
<li id="_tabs_11_xml" class="tab">
<p>XML</p>
</li>
</ul>
</div>
<div id="_tabs_11_java--panel" class="tabpanel" aria-labelledby="_tabs_11_java">
<div class="listingblock primary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Bean
public SecurityFilterChain filterChain(HttpSecurity http) {
    http
        .sessionManagement((session) -&gt; session
            .invalidSessionUrl("/invalidSession")
        );
    return http.build();
}</code></pre>
</div>
</div>
</div>
<div id="_tabs_11_kotlin--panel" class="tabpanel" aria-labelledby="_tabs_11_kotlin">
<div class="listingblock secondary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-kotlin hljs" data-lang="kotlin">@Bean
open fun filterChain(http: HttpSecurity): SecurityFilterChain {
    http {
        sessionManagement {
            invalidSessionUrl = "/invalidSession"
        }
    }
    return http.build()
}</code></pre>
</div>
</div>
</div>
<div id="_tabs_11_xml--panel" class="tabpanel" aria-labelledby="_tabs_11_xml">
<div class="listingblock secondary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-xml hljs" data-lang="xml">&lt;http&gt;
...
&lt;session-management invalid-session-url="/invalidSession" /&gt;
&lt;/http&gt;</code></pre>
</div>
</div>
</div>
</div>
</div>
<div class="paragraph">
<p>Note that if you use this mechanism to detect session timeouts, it may falsely report an error if the user logs out and then logs back in without closing the browser.
This is because the session cookie is not cleared when you invalidate the session and will be resubmitted even if the user has logged out.
If that is your case, you might want to <a href="#clearing-session-cookie-on-logout">configure logout to clear the session cookie</a>.</p>
</div>
<div class="sect2">
<h3 id="_customizing_the_invalid_session_strategy"><a class="anchor" href="#_customizing_the_invalid_session_strategy"></a>Customizing the Invalid Session Strategy</h3>
<div class="paragraph">
<p>The <code>invalidSessionUrl</code> is a convenience method for setting the <code>InvalidSessionStrategy</code> using the <a href="../../api/java/org/springframework/security/web/session/SimpleRedirectInvalidSessionStrategy.html" class="xref attachment apiref"><code>SimpleRedirectInvalidSessionStrategy</code> implementation</a>.
If you want to customize the behavior, you can implement the <a href="../../api/java/org/springframework/security/web/session/InvalidSessionStrategy.html" class="xref attachment apiref"><code>InvalidSessionStrategy</code></a> interface and configure it using the <code>invalidSessionStrategy</code> method:</p>
</div>
<div id="_tabs_12" class="openblock tabs is-sync is-loading">
<div class="content">
<div class="ulist tablist">
<ul>
<li id="_tabs_12_java" class="tab">
<p>Java</p>
</li>
<li id="_tabs_12_kotlin" class="tab">
<p>Kotlin</p>
</li>
<li id="_tabs_12_xml" class="tab">
<p>XML</p>
</li>
</ul>
</div>
<div id="_tabs_12_java--panel" class="tabpanel" aria-labelledby="_tabs_12_java">
<div class="listingblock primary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Bean
public SecurityFilterChain filterChain(HttpSecurity http) {
    http
        .sessionManagement((session) -&gt; session
            .invalidSessionStrategy(new MyCustomInvalidSessionStrategy())
        );
    return http.build();
}</code></pre>
</div>
</div>
</div>
<div id="_tabs_12_kotlin--panel" class="tabpanel" aria-labelledby="_tabs_12_kotlin">
<div class="listingblock secondary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-kotlin hljs" data-lang="kotlin">@Bean
open fun filterChain(http: HttpSecurity): SecurityFilterChain {
    http {
        sessionManagement {
            invalidSessionStrategy = MyCustomInvalidSessionStrategy()
        }
    }
    return http.build()
}</code></pre>
</div>
</div>
</div>
<div id="_tabs_12_xml--panel" class="tabpanel" aria-labelledby="_tabs_12_xml">
<div class="listingblock secondary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-xml hljs" data-lang="xml">&lt;http&gt;
...
&lt;session-management invalid-session-strategy-ref="myCustomInvalidSessionStrategy" /&gt;
&lt;bean name="myCustomInvalidSessionStrategy" class="com.example.MyCustomInvalidSessionStrategy" /&gt;
&lt;/http&gt;</code></pre>
</div>
</div>
</div>
</div>
</div>
</div>
</div>
</div>
<div class="sect1">
<h2 id="clearing-session-cookie-on-logout"><a class="anchor" href="#clearing-session-cookie-on-logout"></a>Clearing Session Cookies on Logout</h2>
<div class="sectionbody">
<div class="paragraph">
<p>You can explicitly delete the JSESSIONID cookie on logging out, for example by using the <a href="https://w3c.github.io/webappsec-clear-site-data/"><code>Clear-Site-Data</code> header</a> in the logout handler:</p>
</div>
<div id="_tabs_13" class="openblock tabs is-sync is-loading">
<div class="content">
<div class="ulist tablist">
<ul>
<li id="_tabs_13_java" class="tab">
<p>Java</p>
</li>
<li id="_tabs_13_kotlin" class="tab">
<p>Kotlin</p>
</li>
<li id="_tabs_13_xml" class="tab">
<p>XML</p>
</li>
</ul>
</div>
<div id="_tabs_13_java--panel" class="tabpanel" aria-labelledby="_tabs_13_java">
<div class="listingblock primary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Bean
public SecurityFilterChain filterChain(HttpSecurity http) {
    http
        .logout((logout) -&gt; logout
            .addLogoutHandler(new HeaderWriterLogoutHandler(new ClearSiteDataHeaderWriter(COOKIES)))
        );
    return http.build();
}</code></pre>
</div>
</div>
</div>
<div id="_tabs_13_kotlin--panel" class="tabpanel" aria-labelledby="_tabs_13_kotlin">
<div class="listingblock secondary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-kotlin hljs" data-lang="kotlin">@Bean
open fun filterChain(http: HttpSecurity): SecurityFilterChain {
    http {
        logout {
            addLogoutHandler(HeaderWriterLogoutHandler(ClearSiteDataHeaderWriter(COOKIES)))
        }
    }
    return http.build()
}</code></pre>
</div>
</div>
</div>
<div id="_tabs_13_xml--panel" class="tabpanel" aria-labelledby="_tabs_13_xml">
<div class="listingblock secondary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-xml hljs" data-lang="xml">&lt;http&gt;
&lt;logout success-handler-ref="clearSiteDataHandler" /&gt;
&lt;b:bean id="clearSiteDataHandler" class="org.springframework.security.web.authentication.logout.HeaderWriterLogoutHandler"&gt;
    &lt;b:constructor-arg&gt;
        &lt;b:bean class="org.springframework.security.web.header.writers.ClearSiteDataHeaderWriter"&gt;
            &lt;b:constructor-arg&gt;
                &lt;b:list&gt;
                    &lt;b:value&gt;COOKIES&lt;/b:value&gt;
                &lt;/b:list&gt;
            &lt;/b:constructor-arg&gt;
        &lt;/b:bean&gt;
    &lt;/b:constructor-arg&gt;
&lt;/b:bean&gt;
&lt;/http&gt;</code></pre>
</div>
</div>
</div>
</div>
</div>
<div class="paragraph">
<p>This has the advantage of being container agnostic and will work with any container that supports the <code>Clear-Site-Data</code> header.</p>
</div>
<div class="paragraph">
<p>As an alternative, you can also use the following syntax in the logout handler:</p>
</div>
<div id="_tabs_14" class="openblock tabs is-sync is-loading">
<div class="content">
<div class="ulist tablist">
<ul>
<li id="_tabs_14_java" class="tab">
<p>Java</p>
</li>
<li id="_tabs_14_kotlin" class="tab">
<p>Kotlin</p>
</li>
<li id="_tabs_14_xml" class="tab">
<p>XML</p>
</li>
</ul>
</div>
<div id="_tabs_14_java--panel" class="tabpanel" aria-labelledby="_tabs_14_java">
<div class="listingblock primary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Bean
public SecurityFilterChain filterChain(HttpSecurity http) {
    http
        .logout((logout) -&gt; logout
            .deleteCookies("JSESSIONID")
        );
    return http.build();
}</code></pre>
</div>
</div>
</div>
<div id="_tabs_14_kotlin--panel" class="tabpanel" aria-labelledby="_tabs_14_kotlin">
<div class="listingblock secondary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-kotlin hljs" data-lang="kotlin">@Bean
open fun filterChain(http: HttpSecurity): SecurityFilterChain {
    http {
        logout {
            deleteCookies("JSESSIONID")
        }
    }
    return http.build()
}</code></pre>
</div>
</div>
</div>
<div id="_tabs_14_xml--panel" class="tabpanel" aria-labelledby="_tabs_14_xml">
<div class="listingblock secondary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-xml hljs" data-lang="xml">&lt;http&gt;
  &lt;logout delete-cookies="JSESSIONID" /&gt;
&lt;/http&gt;</code></pre>
</div>
</div>
</div>
</div>
</div>
<div class="paragraph">
<p>Unfortunately, this cannot be guaranteed to work with every servlet container, so you need to test it in your environment.</p>
</div>
<div class="admonitionblock note">
<table>
<tr>
<td class="icon">
<i class="fa icon-note" title="Note"></i>
</td>
<td class="content">
<div class="paragraph">
<p>If you run your application behind a proxy, you may also be able to remove the session cookie by configuring the proxy server.
For example, by using Apache HTTPD&#8217;s <code>mod_headers</code>, the following directive deletes the <code>JSESSIONID</code> cookie by expiring it in the response to a logout request (assuming the application is deployed under the <code>/tutorial</code> path):</p>
</div>
</td>
</tr>
</table>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-xml hljs" data-lang="xml">&lt;LocationMatch "/tutorial/logout"&gt;
Header always set Set-Cookie "JSESSIONID=;Path=/tutorial;Expires=Thu, 01 Jan 1970 00:00:00 GMT"
&lt;/LocationMatch&gt;</code></pre>
</div>
</div>
<div class="paragraph">
<p>More details on the <a href="../exploits/headers.html#servlet-headers-clear-site-data" class="xref page">Clear Site Data</a> and <a href="logout.html" class="xref page">Logout sections</a>.</p>
</div>
</div>
</div>
<div class="sect1">
<h2 id="ns-session-fixation"><a class="anchor" href="#ns-session-fixation"></a>Understanding Session Fixation Attack Protection</h2>
<div class="sectionbody">
<div class="paragraph">
<p><a href="https://en.wikipedia.org/wiki/Session_fixation">Session fixation</a> attacks are a potential risk where it is possible for a malicious attacker to create a session by accessing a site, then persuade another user to log in with the same session (by sending them a link containing the session identifier as a parameter, for example).
Spring Security protects against this automatically by creating a new session or otherwise changing the session ID when a user logs in.</p>
</div>
<div class="sect2">
<h3 id="_configuring_session_fixation_protection"><a class="anchor" href="#_configuring_session_fixation_protection"></a>Configuring Session Fixation Protection</h3>
<div class="paragraph">
<p>You can control the strategy for Session Fixation Protection by choosing between three recommended options:</p>
</div>
<div class="ulist">
<ul>
<li>
<p><code>changeSessionId</code> - Do not create a new session.
Instead, use the session fixation protection provided by the Servlet container (<code>HttpServletRequest#changeSessionId()</code>).
This option is only available in Servlet 3.1 (Java EE 7) and newer containers.
Specifying it in older containers will result in an exception.
This is the default in Servlet 3.1 and newer containers.</p>
</li>
<li>
<p><code>newSession</code> - Create a new "clean" session, without copying the existing session data (Spring Security-related attributes will still be copied).</p>
</li>
<li>
<p><code>migrateSession</code> - Create a new session and copy all existing session attributes to the new session.
This is the default in Servlet 3.0 or older containers.</p>
</li>
</ul>
</div>
<div class="paragraph">
<p>You can configure the session fixation protection by doing:</p>
</div>
<div id="_tabs_15" class="openblock tabs is-sync is-loading">
<div class="content">
<div class="ulist tablist">
<ul>
<li id="_tabs_15_java" class="tab">
<p>Java</p>
</li>
<li id="_tabs_15_kotlin" class="tab">
<p>Kotlin</p>
</li>
<li id="_tabs_15_xml" class="tab">
<p>XML</p>
</li>
</ul>
</div>
<div id="_tabs_15_java--panel" class="tabpanel" aria-labelledby="_tabs_15_java">
<div class="listingblock primary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Bean
public SecurityFilterChain filterChain(HttpSecurity http) {
    http
        .sessionManagement((session) -&gt; session
            .sessionFixation((sessionFixation) -&gt; sessionFixation
                .newSession()
            )
        );
    return http.build();
}</code></pre>
</div>
</div>
</div>
<div id="_tabs_15_kotlin--panel" class="tabpanel" aria-labelledby="_tabs_15_kotlin">
<div class="listingblock secondary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-kotlin hljs" data-lang="kotlin">@Bean
open fun filterChain(http: HttpSecurity): SecurityFilterChain {
    http {
        sessionManagement {
            sessionFixation {
                newSession()
            }
        }
    }
    return http.build()
}</code></pre>
</div>
</div>
</div>
<div id="_tabs_15_xml--panel" class="tabpanel" aria-labelledby="_tabs_15_xml">
<div class="listingblock secondary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-xml hljs" data-lang="xml">&lt;http&gt;
  &lt;session-management session-fixation-protection="newSession" /&gt;
&lt;/http&gt;</code></pre>
</div>
</div>
</div>
</div>
</div>
<div class="paragraph">
<p>When session fixation protection occurs, it results in a <code>SessionFixationProtectionEvent</code> being published in the application context.
If you use <code>changeSessionId</code>, this protection will <em>also</em> result in any  <code>jakarta.servlet.http.HttpSessionIdListener</code>s being notified, so use caution if your code listens for both events.</p>
</div>
<div class="paragraph">
<p>You can also set the session fixation protection to <code>none</code> to disable it, but this is not recommended as it leaves your application vulnerable.</p>
</div>
</div>
</div>
</div>
<div class="sect1">
<h2 id="use-securitycontextholderstrategy"><a class="anchor" href="#use-securitycontextholderstrategy"></a>Using <code>SecurityContextHolderStrategy</code></h2>
<div class="sectionbody">
<div class="paragraph">
<p>Consider the following block of code:</p>
</div>
<div id="_tabs_16" class="openblock tabs is-sync is-loading">
<div class="content">
<div class="ulist tablist">
<ul>
<li id="_tabs_16_java" class="tab">
<p>Java</p>
</li>
</ul>
</div>
<div id="_tabs_16_java--panel" class="tabpanel" aria-labelledby="_tabs_16_java">
<div class="listingblock primary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
        loginRequest.getUsername(), loginRequest.getPassword());
Authentication authentication = this.authenticationManager.authenticate(token);
// ...
SecurityContext context = SecurityContextHolder.createEmptyContext(); <i class="conum" data-value="1"></i><b>(1)</b>
context.setAuthentication(authentication); <i class="conum" data-value="2"></i><b>(2)</b>
SecurityContextHolder.setContext(context); <i class="conum" data-value="3"></i><b>(3)</b></code></pre>
</div>
</div>
</div>
</div>
</div>
<div class="olist arabic">
<ol class="arabic">
<li>
<p>Creates an empty <code>SecurityContext</code> instance by accessing the <code>SecurityContextHolder</code> statically.</p>
</li>
<li>
<p>Sets the <code>Authentication</code> object in the <code>SecurityContext</code> instance.</p>
</li>
<li>
<p>Sets the <code>SecurityContext</code> instance in the <code>SecurityContextHolder</code> statically.</p>
</li>
</ol>
</div>
<div class="paragraph">
<p>While the above code works fine, it can produce some undesired effects: when components access the <code>SecurityContext</code> statically through <code>SecurityContextHolder</code>, this can create race conditions when there are multiple application contexts that want to specify the <code>SecurityContextHolderStrategy</code>.
This is because in <code>SecurityContextHolder</code> there is one strategy per classloader instead of one per application context.</p>
</div>
<div class="paragraph">
<p>To address this, components can wire <code>SecurityContextHolderStrategy</code> from the application context.
By default, they will still look up the strategy from <code>SecurityContextHolder</code>.</p>
</div>
<div class="paragraph">
<p>These changes are largely internal, but they present the opportunity for applications to autowire the <code>SecurityContextHolderStrategy</code> instead of accessing the <code>SecurityContext</code> statically.
To do so, you should change the code to the following:</p>
</div>
<div id="_tabs_17" class="openblock tabs is-sync is-loading">
<div class="content">
<div class="ulist tablist">
<ul>
<li id="_tabs_17_java" class="tab">
<p>Java</p>
</li>
</ul>
</div>
<div id="_tabs_17_java--panel" class="tabpanel" aria-labelledby="_tabs_17_java">
<div class="listingblock primary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">public class SomeClass {

    private final SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();

    public void someMethod() {
        UsernamePasswordAuthenticationToken token = UsernamePasswordAuthenticationToken.unauthenticated(
                loginRequest.getUsername(), loginRequest.getPassword());
        Authentication authentication = this.authenticationManager.authenticate(token);
        // ...
        SecurityContext context = this.securityContextHolderStrategy.createEmptyContext(); <i class="conum" data-value="1"></i><b>(1)</b>
        context.setAuthentication(authentication); <i class="conum" data-value="2"></i><b>(2)</b>
        this.securityContextHolderStrategy.setContext(context); <i class="conum" data-value="3"></i><b>(3)</b>
    }

}</code></pre>
</div>
</div>
</div>
</div>
</div>
<div class="olist arabic">
<ol class="arabic">
<li>
<p>Creates an empty <code>SecurityContext</code> instance using the configured <code>SecurityContextHolderStrategy</code>.</p>
</li>
<li>
<p>Sets the <code>Authentication</code> object in the <code>SecurityContext</code> instance.</p>
</li>
<li>
<p>Sets the <code>SecurityContext</code> instance in the <code>SecurityContextHolderStrategy</code>.</p>
</li>
</ol>
</div>
</div>
</div>
<div class="sect1">
<h2 id="session-mgmt-force-session-creation"><a class="anchor" href="#session-mgmt-force-session-creation"></a>Forcing Eager Session Creation</h2>
<div class="sectionbody">
<div class="paragraph">
<p>At times, it can be valuable to eagerly create sessions.
This can be done by using the <a href="../../api/java/org/springframework/security/web/session/ForceEagerSessionCreationFilter.html" class="xref attachment apiref"><code>ForceEagerSessionCreationFilter</code></a> which can be configured using:</p>
</div>
<div id="_tabs_18" class="openblock tabs is-sync is-loading">
<div class="content">
<div class="ulist tablist">
<ul>
<li id="_tabs_18_java" class="tab">
<p>Java</p>
</li>
<li id="_tabs_18_kotlin" class="tab">
<p>Kotlin</p>
</li>
<li id="_tabs_18_xml" class="tab">
<p>XML</p>
</li>
</ul>
</div>
<div id="_tabs_18_java--panel" class="tabpanel" aria-labelledby="_tabs_18_java">
<div class="listingblock primary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Bean
public SecurityFilterChain filterChain(HttpSecurity http) {
    http
        .sessionManagement((session) -&gt; session
            .sessionCreationPolicy(SessionCreationPolicy.ALWAYS)
        );
    return http.build();
}</code></pre>
</div>
</div>
</div>
<div id="_tabs_18_kotlin--panel" class="tabpanel" aria-labelledby="_tabs_18_kotlin">
<div class="listingblock secondary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-kotlin hljs" data-lang="kotlin">@Bean
open fun filterChain(http: HttpSecurity): SecurityFilterChain {
    http {
        sessionManagement {
            sessionCreationPolicy = SessionCreationPolicy.ALWAYS
        }
    }
    return http.build()
}</code></pre>
</div>
</div>
</div>
<div id="_tabs_18_xml--panel" class="tabpanel" aria-labelledby="_tabs_18_xml">
<div class="listingblock secondary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-xml hljs" data-lang="xml">&lt;http create-session="ALWAYS"&gt;

&lt;/http&gt;</code></pre>
</div>
</div>
</div>
</div>
</div>
</div>
</div>
<div class="sect1">
<h2 id="_what_to_read_next"><a class="anchor" href="#_what_to_read_next"></a>What to read next</h2>
<div class="sectionbody">
<div class="ulist">
<ul>
<li>
<p>Clustered sessions with <a href="https://docs.spring.io/spring-session/reference/index.html">Spring Session</a></p>
</li>
</ul>
</div>
</div>
</div>
<div id="footnotes">
<hr>
<div class="footnote" id="_footnotedef_1">
<a href="#_footnoteref_1">1</a>. Authentication by mechanisms which perform a redirect after authenticating (such as form-login) will not be detected by <code>SessionManagementFilter</code>, as the filter will not be invoked during the authenticating request. Session-management functionality has to be handled separately in these cases.
</div>
</div>
</article>  </div>
</main>
<div class="modal micromodal-slide" id="modal-versions" aria-hidden="true">
    <div class="modal__overlay" tabindex="-1" data-micromodal-close>
        <div class="modal__container" role="dialog" aria-modal="true">
            <main class="modal__content" id="modal-versions-content">
              <button data-micromodal-close class="modal-versions-close">
                <svg width="28px" height="28px" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 32 32"><defs><style>.cls-1h{fill:none;stroke:#000;stroke-linecap:round;stroke-linejoin:round;stroke-width:2px;}</style></defs><title/><g id="cross"><line class="cls-1h" x1="7" x2="25" y1="7" y2="25"/><line class="cls-1h" x1="7" x2="25" y1="25" y2="7"/></g></svg>
              </button>
              <div class="colset">
                <div class="col-left">

                  <ul class="nav-versions">
                      <li class="component">
                        <div>
                          <a class="title" href="../../index.html">Spring Security</a>
                        </div>                        <div class="version-item is-active">
                          <div>
                            <button class="version-toggle" type="button">
                              <span></span>
                              Stable
                            </button>
                          </div>
                          <ul class="versions">
  <li class="version">
    <a href="session-management.html">
      7.1.1
    </a>
  </li>
  <li class="version">
    <a href="../../7.0/servlet/authentication/session-management.html">
      7.0.7
    </a>
  </li>
  <li class="version">
    <a href="../../6.5/servlet/authentication/session-management.html">
      6.5.11
    </a>
  </li>
</ul>                        </div>
                        <div class="version-item">
                          <div>
                            <button class="version-toggle" type="button">
                              <span></span>
                              Preview
                            </button>
                          </div>
                          <ul class="versions">
  <li class="version">
    <a href="../../7.2/servlet/authentication/session-management.html">
      7.2.0-M1
    </a>
  </li>
</ul>                        </div>
                        <div class="version-item">
                          <div>
                            <button class="version-toggle" type="button">
                              <span></span>
                              Snapshot
                            </button>
                          </div>
                          <ul class="versions">
  <li class="version">
    <a href="../../7.2-SNAPSHOT/servlet/authentication/session-management.html">
      7.2.0-SNAPSHOT
    </a>
  </li>
  <li class="version">
    <a href="../../7.1-SNAPSHOT/servlet/authentication/session-management.html">
      7.1.2-SNAPSHOT
    </a>
  </li>
  <li class="version">
    <a href="../../7.0-SNAPSHOT/servlet/authentication/session-management.html">
      7.0.8-SNAPSHOT
    </a>
  </li>
  <li class="version">
    <a href="../../6.5-SNAPSHOT/servlet/authentication/session-management.html">
      6.5.12-SNAPSHOT
    </a>
  </li>
</ul>                        </div>
                        
                      </li>
                  </ul>
                </div>
                <div class="col-right">
                  <ul class="projects">
  <li>
    Related Spring Documentation
    <ul class="projects-list">
        <li>
<a href="https://docs.spring.io/spring-framework/reference/">
  Spring Framework
</a>
</li>
        <li>
<a class="anchor"><i class="fa fa-angle-right" aria-hidden="true"></i></a>
<a href="https://docs.spring.io/spring-security/reference/">
  Spring Security
</a>
<ul>
    <li>
<a href="https://docs.spring.io/spring-authorization-server/reference/">
  Spring Authorization Server
</a>
</li>
    <li>
<a href="https://docs.spring.io/spring-ldap/reference/">
  Spring LDAP
</a>
</li>
    <li>
<a href="https://docs.spring.io/spring-security-kerberos/reference/">
  Spring Security Kerberos
</a>
</li>
    <li>
<a href="https://docs.spring.io/spring-session/reference/">
  Spring Session
</a>
</li>
    <li>
<a href="https://docs.spring.io/spring-vault/reference/">
  Spring Vault
</a>
</li>
</ul>
</li>
        <li>
<a href="https://docs.spring.io/spring-graphql/reference/">
  Spring GraphQL
</a>
</li>
    </ul>
  </li
  <li><a href="../../spring-projects.html">All Docs...</a></li>
</ul>
                </div>
              </div>
            </main>
        </div>
    </div>
</div>

</div>
<footer class="footer flex">
    <div id="spring-links flex">
        <img id="springlogo" src="../../_/img/spring-logo.svg" alt="Spring">
        <p class="smallest antialiased">Copyright © 2005 - <script>var d = new Date();
        document.write(d.getFullYear());</script> Broadcom. All Rights Reserved. The term "Broadcom" refers to Broadcom Inc. and/or its subsidiaries.<br /><a href="https://www.vmware.com/help/legal.html">Terms of Use</a> • <a href="https://www.vmware.com/help/privacy.html" rel="noopener noreferrer">Privacy</a> • <a href="https://spring.io/trademarks">Trademark Guidelines</a> <span id="thank-you-mobile">• <a href="https://spring.io/thank-you">Thank you</a></span> • <a href="https://www.vmware.com/help/privacy/california-privacy-rights.html">Your California Privacy Rights</a> • <a class="ot-sdk-show-settings">Cookie Settings</a> <span id="teconsent"></span></p>
        <p class="smallest antialiased has-gray-text">Apache®, Apache Tomcat®, Apache Kafka®, Apache Cassandra&trade;, and Apache Geode&trade; are trademarks or registered trademarks of the Apache Software Foundation in the United States and/or other countries. Java&trade;, Java&trade; SE, Java&trade; EE, and OpenJDK&trade; are trademarks of Oracle and/or its affiliates. Kubernetes® is a registered trademark of the Linux Foundation in the United States and other countries. Linux® is the registered trademark of Linus Torvalds in the United States and other countries. Windows® and Microsoft® Azure are registered trademarks of Microsoft Corporation. “AWS” and “Amazon Web Services” are trademarks or registered trademarks of Amazon.com Inc. or its affiliates. All other trademarks and copyrights are property of their respective owners and are only mentioned for informative purposes. Other names may be trademarks of their respective owners.</p>
    </div>
    <div id="social-icons" class="flex jc-between">
        <a href="https://www.youtube.com/user/SpringSourceDev" title="Youtube"><svg id="youtube-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 40 40"><circle class="cls-1" cx="20" cy="20" r="20"/><path class="cls-2" d="M30.91,14.53a2.89,2.89,0,0,0-2-2C27.12,12,20,12,20,12s-7.12,0-8.9.47a2.9,2.9,0,0,0-2,2A30.56,30.56,0,0,0,8.63,20a30.44,30.44,0,0,0,.46,5.47,2.89,2.89,0,0,0,2,2C12.9,28,20,28,20,28s7.12,0,8.9-.47a2.87,2.87,0,0,0,2-2A30.56,30.56,0,0,0,31.37,20,28.88,28.88,0,0,0,30.91,14.53ZM17.73,23.41V16.59L23.65,20Z"/></svg></a>
        <a href="https://github.com/spring-projects" title="GitHub"><svg id="github-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 75.93 75.93"><path class="cls-1" d="M38,0a38,38,0,1,0,38,38A38,38,0,0,0,38,0Z"/></g><path class="cls-2" d="M38,15.59A22.95,22.95,0,0,0,30.71,60.3c1.15.21,1.57-.5,1.57-1.11s0-2,0-3.9c-6.38,1.39-7.73-3.07-7.73-3.07A6.09,6.09,0,0,0,22,48.86c-2.09-1.42.15-1.39.15-1.39a4.81,4.81,0,0,1,3.52,2.36c2,3.5,5.37,2.49,6.67,1.91a4.87,4.87,0,0,1,1.46-3.07c-5.09-.58-10.45-2.55-10.45-11.34a8.84,8.84,0,0,1,2.36-6.15,8.29,8.29,0,0,1,.23-6.07s1.92-.62,6.3,2.35a21.82,21.82,0,0,1,11.49,0c4.38-3,6.3-2.35,6.3-2.35a8.29,8.29,0,0,1,.23,6.07,8.84,8.84,0,0,1,2.36,6.15c0,8.81-5.37,10.75-10.48,11.32a5.46,5.46,0,0,1,1.56,4.25c0,3.07,0,5.54,0,6.29s.42,1.33,1.58,1.1A22.94,22.94,0,0,0,38,15.59Z"/></svg></a>
        <a href="https://twitter.com/springcentral" title="Twitter"><svg id="twitter-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 75.93 75.93"><circle class="cls-1" cx="37.97" cy="37.97" r="37.97"/><path id="Twitter-2" data-name="Twitter" class="cls-2" d="M55.2,22.73a15.43,15.43,0,0,1-4.88,1.91,7.56,7.56,0,0,0-5.61-2.49A7.78,7.78,0,0,0,37,30a7.56,7.56,0,0,0,.2,1.79,21.63,21.63,0,0,1-15.84-8.23,8,8,0,0,0,2.37,10.52,7.66,7.66,0,0,1-3.48-1v.09A7.84,7.84,0,0,0,26.45,41a7.54,7.54,0,0,1-2,.28A7.64,7.64,0,0,1,23,41.09a7.71,7.71,0,0,0,7.18,5.47,15.21,15.21,0,0,1-9.55,3.37,15.78,15.78,0,0,1-1.83-.11,21.41,21.41,0,0,0,11.78,3.54c14.13,0,21.86-12,21.86-22.42,0-.34,0-.68,0-1a15.67,15.67,0,0,0,3.83-4.08,14.9,14.9,0,0,1-4.41,1.24A7.8,7.8,0,0,0,55.2,22.73Z"/></svg></a>
    </div>
</footer>
<script src="../../_/js/vendor/import.js"></script>
<script src="../../_/js/site.js"></script>
<script async src="../../_/js/vendor/highlight.js"></script>
<script async src="../../_/js/vendor/asciidoctor-tabs.js" data-sync-storage-key="docs:preferred-tab"></script>

<div class="modal micromodal-slide" id="modal-1" aria-hidden="true">
    <div class="modal__overlay" tabindex="-1" data-micromodal-close>
        <div class="modal__container" role="dialog" aria-modal="true" aria-labelledby="modal-1-title">
            <main class="modal__content" id="modal-1-content">
                <div id="searchbox"></div>
                <div id="counter"></div>
                <div class="search-link-box">
                    <a class="search-link" href="../../search.html">Search in all Spring Docs</a>
                </div>
                <div class="search-by">
                    <a target="_blank" rel="noopener noreferrer" href="https://www.algolia.com/" aria-label="Search by Algolia">
                        <img class="light" width="140" src="../../_/img/algolia-light.svg" />
                        <img class="dark" width="140" src="../../_/img/algolia-dark.svg" />
                    </a>
                </div>
                <div id="hits"></div>
            </main>
        </div>
    </div>
</div>

<script src="../../_/js/vendor/hotkeys.js"></script>
<script src="https://cdn.jsdelivr.net/npm/algoliasearch@4.17.0/dist/algoliasearch-lite.umd.js" integrity="sha256-Lf9DrpGmcRip6OQzbcL6lnvNmoZNSKpyQX5pMlwatWE=" crossorigin="anonymous"></script>
<script src="https://cdn.jsdelivr.net/npm/instantsearch.js@4.54.1/dist/instantsearch.production.min.js" integrity="sha256-xYsZPDeNjYNTBWLvqD2Lxe98hOxcDgOHyMPfz4tVAbk=" crossorigin="anonymous"></script>
<script async id="search-script" src="../../_/js/vendor/search.js" data-app-id="WB1FQYI187" data-api-key="c2e84f15fa630d534f1c62b1c413bb77" data-index-name="springdocs" data-stylesheet="../../_/css/vendor/search.css" data-page-version="7.1.1" data-page-generation="7.1" data-page-component="security"></script>
  </body>
</html>

<!DOCTYPE html>
<html lang="en">
  <script src="https://cdn.cookielaw.org/scripttemplates/otSDKStub.js" data-domain-script="018ee325-b3a7-7753-937b-b8b3e643b1a7"></script><script>function OptanonWrapper() {}</script><script>function setGTM(w, d, s, l, i) { w[l] = w[l] || []; w[l].push({ "gtm.start": new Date().getTime(), event: "gtm.js"}); var f = d.getElementsByTagName(s)[0], j = d.createElement(s), dl = l != "dataLayer" ? "&l=" + l : ""; j.async = true; j.src = "https://www.googletagmanager.com/gtm.js?id=" + i + dl; f.parentNode.insertBefore(j, f); } if (document.cookie.indexOf("OptanonConsent") > -1 && document.cookie.indexOf("groups=") > -1) { setGTM(window, document, "script", "dataLayer", "GTM-W8CQ8TL"); } else { waitForOnetrustActiveGroups(); } var timer; function waitForOnetrustActiveGroups() { if (document.cookie.indexOf("OptanonConsent") > -1 && document.cookie.indexOf("groups=") > -1) { clearTimeout(timer); setGTM(window, document, "script", "dataLayer", "GTM-W8CQ8TL"); } else { timer = setTimeout(waitForOnetrustActiveGroups, 250); }}</script>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>Cross Site Request Forgery (CSRF) :: Spring Security</title>
    <link rel="canonical" href="https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html">
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
    <meta name="versioned-url" content="https://docs.spring.io/spring-security/reference/7.1/servlet/exploits/csrf.html">
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
    <a class="nav-link"  href="../authentication/index.html">Authentication</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../authentication/architecture.html">Authentication Architecture</a>
  </li>
  <li class="nav-item" data-depth="3">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../authentication/passwords/index.html">Username/Password</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="4">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../authentication/passwords/input.html">Reading Username/Password</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../authentication/passwords/form.html">Form</a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../authentication/passwords/basic.html">Basic</a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../authentication/passwords/digest.html">Digest</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="4">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../authentication/passwords/storage.html">Password Storage</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../authentication/passwords/in-memory.html">In Memory</a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../authentication/passwords/jdbc.html">JDBC</a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../authentication/passwords/user-details.html">UserDetails</a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../authentication/passwords/credentials-container.html">CredentialsContainer</a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../authentication/passwords/erasure.html">Password Erasure</a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../authentication/passwords/user-details-service.html">UserDetailsService</a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../authentication/passwords/password-encoder.html">PasswordEncoder</a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../authentication/passwords/dao-authentication-provider.html">DaoAuthenticationProvider</a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../authentication/passwords/ldap.html">LDAP</a>
  </li>
</ul>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../authentication/mfa.html">Multi-Factor Authentication</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../authentication/persistence.html">Persistence</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../authentication/passkeys.html">Passkeys</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../authentication/onetimetoken.html">One-Time Token</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../authentication/session-management.html">Session Management</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../authentication/rememberme.html">Remember Me</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../authentication/anonymous.html">Anonymous</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../authentication/preauth.html">Pre-Authentication</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../authentication/jaas.html">JAAS</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../authentication/cas.html">CAS</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../authentication/x509.html">X509</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../authentication/runas.html">Run-As</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../authentication/logout.html">Logout</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../authentication/events.html">Authentication Events</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../authentication/kerberos/index.html">Kerberos</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../authentication/kerberos/introduction.html">Introduction</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../authentication/kerberos/ssk.html">Reference</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../authentication/kerberos/samples.html">Samples</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../authentication/kerberos/appendix.html">Appendices</a>
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
    <a class="nav-link"  href="index.html">Protection Against Exploits</a>
<ul class="nav-list">
  <li class="nav-item is-current-page" data-depth="3">
    <a class="nav-link"  href="csrf.html">Cross Site Request Forgery (CSRF)</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="headers.html">Security HTTP Response Headers</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="http.html">HTTP</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="firewall.html">HttpFirewall</a>
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
      data-title="Cross Site Request Forgery (CSRF)"
      data-levels="2"
    >
      <div class="toc-menu"></div>
    </div>
    <div class="sidebar-links">
        <a href="https://github.com/spring-projects/spring-security/blob/7.1.1/docs/modules/ROOT/pages/servlet/exploits/csrf.adoc">
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
      <li><a href="index.html">Protection Against Exploits</a></li>
      <li><a href="csrf.html">Cross Site Request Forgery (CSRF)</a></li>
    </ul>
  </nav>
</div><h1 id="page-title" class="page">Cross Site Request Forgery (CSRF)</h1>
<div id="preamble">
<div class="sectionbody">
<div class="paragraph">
<p>In an application where end users can <a href="../authentication/index.html" class="xref page">log in</a>, it is important to consider how to protect against <a href="../../features/exploits/csrf.html#csrf" class="xref page">Cross Site Request Forgery (CSRF)</a>.</p>
</div>
<div class="paragraph">
<p>Spring Security protects against CSRF attacks by default for <a href="../../features/exploits/csrf.html#csrf-protection-read-only" class="xref page">unsafe HTTP methods</a>, such as a POST request, so no additional code is necessary.
You can specify the default configuration explicitly using the following:</p>
</div>
<div id="csrf-configuration" class="openblock tabs is-sync is-loading">
<div class="title">Configure CSRF Protection</div>
<div class="content">
<div class="ulist tablist">
<ul>
<li id="csrf_configuration_java" class="tab">
<p>Java</p>
</li>
<li id="csrf_configuration_kotlin" class="tab">
<p>Kotlin</p>
</li>
<li id="csrf_configuration_xml" class="tab">
<p>XML</p>
</li>
</ul>
</div>
<div id="csrf_configuration_java--panel" class="tabpanel" aria-labelledby="csrf_configuration_java">
<div class="listingblock primary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			// ...
			.csrf(Customizer.withDefaults());
		return http.build();
	}
}</code></pre>
</div>
</div>
</div>
<div id="csrf_configuration_kotlin--panel" class="tabpanel" aria-labelledby="csrf_configuration_kotlin">
<div class="listingblock secondary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-kotlin hljs" data-lang="kotlin"><span class="fold-block is-hidden-folded">import org.springframework.security.config.annotation.web.invoke

</span><span class="fold-block">@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    open fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            // ...
            csrf { }
        }
        return http.build()
    }
}</span></code></pre>
</div>
</div>
</div>
<div id="csrf_configuration_xml--panel" class="tabpanel" aria-labelledby="csrf_configuration_xml">
<div class="listingblock secondary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-xml hljs" data-lang="xml">&lt;http&gt;
	&lt;!-- ... --&gt;
	&lt;csrf/&gt;
&lt;/http&gt;</code></pre>
</div>
</div>
</div>
</div>
</div>
<div class="paragraph">
<p>To learn more about CSRF protection for your application, consider the following use cases:</p>
</div>
<div class="ulist">
<ul>
<li>
<p>I want to <a href="#csrf-components">understand CSRF protection&#8217;s components</a></p>
</li>
<li>
<p>I need to <a href="#migrating-to-spring-security-6">migrate an application from Spring Security 5 to 6</a></p>
</li>
<li>
<p>I want to <a href="#csrf-token-repository-cookie">store the <code>CsrfToken</code> in a cookie</a> instead of <a href="#csrf-token-repository-httpsession">the session</a></p>
</li>
<li>
<p>I want to <a href="#csrf-token-repository-custom">store the <code>CsrfToken</code> in a custom location</a></p>
</li>
<li>
<p>I want to <a href="#deferred-csrf-token-opt-out">opt-out of deferred tokens</a></p>
</li>
<li>
<p>I want to <a href="#csrf-token-request-handler-opt-out-of-breach">opt-out of BREACH protection</a></p>
</li>
<li>
<p>I need guidance integrating <a href="#csrf-integration-form">Thymeleaf, JSPs or another view technology</a> with the backend</p>
</li>
<li>
<p>I need guidance integrating <a href="#csrf-integration-javascript">Angular or another JavaScript framework</a> with the backend</p>
</li>
<li>
<p>I need guidance integrating <a href="#csrf-integration-mobile">a mobile application or another client</a> with the backend</p>
</li>
<li>
<p>I need guidance on <a href="#csrf-access-denied-handler">handling errors</a></p>
</li>
<li>
<p>I want to <a href="#csrf-testing">test CSRF protection</a></p>
</li>
<li>
<p>I need guidance on <a href="#disable-csrf">disabling CSRF protection</a></p>
</li>
</ul>
</div>
</div>
</div>
<div class="sect1">
<h2 id="csrf-components"><a class="anchor" href="#csrf-components"></a>Understanding CSRF Protection&#8217;s Components</h2>
<div class="sectionbody">
<div class="paragraph">
<p>CSRF protection is provided by several components that are composed within the <a href="../../api/java/org/springframework/security/web/csrf/CsrfFilter.html" class="xref attachment apiref"><code>CsrfFilter</code></a>:</p>
</div>
<div class="imageblock invert-dark">
<div class="content">
<img src="../../_images/servlet/exploits/csrf.png" alt="csrf">
</div>
<div class="title">Figure 1. <code>CsrfFilter</code> Components</div>
</div>
<div class="paragraph">
<p>CSRF protection is divided into two parts:</p>
</div>
<div class="olist arabic">
<ol class="arabic">
<li>
<p>Make the <a href="../../api/java/org/springframework/security/web/csrf/CsrfToken.html" class="xref attachment apiref"><code>CsrfToken</code></a> available to the application by delegating to the <a href="#csrf-token-request-handler"><code>CsrfTokenRequestHandler</code></a>.</p>
</li>
<li>
<p>Determine if the request requires CSRF protection, load and validate the token, and <a href="#csrf-access-denied-handler">handle <code>AccessDeniedException</code></a>.</p>
</li>
</ol>
</div>
<div class="imageblock invert-dark">
<div class="content">
<img src="../../_images/servlet/exploits/csrf-processing.png" alt="csrf processing">
</div>
<div class="title">Figure 2. <code>CsrfFilter</code> Processing</div>
</div>
<div class="ulist">
<ul>
<li>
<p><span class="image"><img src="../../_images/icons/number_1.png" alt="number 1"></span> First, the <a href="../../api/java/org/springframework/security/web/csrf/DeferredCsrfToken.html" class="xref attachment apiref"><code>DeferredCsrfToken</code></a> is loaded, which holds a reference to the <a href="#csrf-token-repository"><code>CsrfTokenRepository</code></a> so that the persisted <code>CsrfToken</code> can be loaded later (in <span class="image"><img src="../../_images/icons/number_4.png" alt="number 4"></span>).</p>
</li>
<li>
<p><span class="image"><img src="../../_images/icons/number_2.png" alt="number 2"></span> Second, a <code>Supplier&lt;CsrfToken&gt;</code> (created from <code>DeferredCsrfToken</code>) is given to the <a href="#csrf-token-request-handler"><code>CsrfTokenRequestHandler</code></a>, which is responsible for populating a request attribute to make the <code>CsrfToken</code> available to the rest of the application.</p>
</li>
<li>
<p><span class="image"><img src="../../_images/icons/number_3.png" alt="number 3"></span> Next, the main CSRF protection processing begins and checks if the current request requires CSRF protection. If not required, the filter chain is continued and processing ends.</p>
</li>
<li>
<p><span class="image"><img src="../../_images/icons/number_4.png" alt="number 4"></span> If CSRF protection is required, the persisted <code>CsrfToken</code> is finally loaded from the <code>DeferredCsrfToken</code>.</p>
</li>
<li>
<p><span class="image"><img src="../../_images/icons/number_5.png" alt="number 5"></span> Continuing, the actual CSRF token provided by the client (if any) is resolved using the <a href="#csrf-token-request-handler"><code>CsrfTokenRequestHandler</code></a>.</p>
</li>
<li>
<p><span class="image"><img src="../../_images/icons/number_6.png" alt="number 6"></span> The actual CSRF token is compared against the persisted <code>CsrfToken</code>. If valid, the filter chain is continued and processing ends.</p>
</li>
<li>
<p><span class="image"><img src="../../_images/icons/number_7.png" alt="number 7"></span> If the actual CSRF token is invalid (or missing), an <code>AccessDeniedException</code> is passed to the <a href="#csrf-access-denied-handler"><code>AccessDeniedHandler</code></a> and processing ends.</p>
</li>
</ul>
</div>
</div>
</div>
<div class="sect1">
<h2 id="migrating-to-spring-security-6"><a class="anchor" href="#migrating-to-spring-security-6"></a>Migrating to Spring Security 6</h2>
<div class="sectionbody">
<div class="paragraph">
<p>When migrating from Spring Security 5 to 6, there are a few changes that may impact your application.
The following is an overview of the aspects of CSRF protection that have changed in Spring Security 6:</p>
</div>
<div class="ulist">
<ul>
<li>
<p>Loading of the <code>CsrfToken</code> is now <a href="#deferred-csrf-token">deferred by default</a> to improve performance by no longer requiring the session to be loaded on every request.</p>
</li>
<li>
<p>The <code>CsrfToken</code> now includes <a href="#csrf-token-request-handler-breach">randomness on every request by default</a> to protect the CSRF token from a <a href="https://en.wikipedia.org/wiki/BREACH">BREACH</a> attack.</p>
</li>
</ul>
</div>
<div class="admonitionblock tip">
<table>
<tr>
<td class="icon">
<i class="fa icon-tip" title="Tip"></i>
</td>
<td class="content">
<div class="paragraph">
<p>The changes in Spring Security 6 require additional configuration for single-page applications, and as such you may find the <a href="#csrf-integration-javascript-spa">Single-Page Applications</a> section particularly useful.</p>
</div>
</td>
</tr>
</table>
</div>
<div class="paragraph">
<p>See the <a href="https://docs.spring.io/spring-security/reference/5.8/migration/servlet/exploits.html">Exploit Protection</a> section of the <a href="https://docs.spring.io/spring-security/reference/5.8/migration/index.html">Migration</a> chapter for more information on migrating a Spring Security 5 application.</p>
</div>
</div>
</div>
<div class="sect1">
<h2 id="csrf-token-repository"><a class="anchor" href="#csrf-token-repository"></a>Persisting the <code>CsrfToken</code></h2>
<div class="sectionbody">
<div class="paragraph">
<p>The <code>CsrfToken</code> is persisted using a <code>CsrfTokenRepository</code>.</p>
</div>
<div class="paragraph">
<p>By default, the <a href="#csrf-token-repository-httpsession"><code>HttpSessionCsrfTokenRepository</code></a> is used for storing tokens in a session.
Spring Security also provides the <a href="#csrf-token-repository-cookie"><code>CookieCsrfTokenRepository</code></a> for storing tokens in a cookie.
You can also specify <a href="#csrf-token-repository-custom">your own implementation</a> to store tokens wherever you like.</p>
</div>
<div class="sect2">
<h3 id="csrf-token-repository-httpsession"><a class="anchor" href="#csrf-token-repository-httpsession"></a>Using the <code>HttpSessionCsrfTokenRepository</code></h3>
<div class="paragraph">
<p>By default, Spring Security stores the expected CSRF token in the <code>HttpSession</code> by using <a href="../../api/java/org/springframework/security/web/csrf/HttpSessionCsrfTokenRepository.html" class="xref attachment apiref"><code>HttpSessionCsrfTokenRepository</code></a>, so no additional code is necessary.</p>
</div>
<div class="paragraph">
<p>The <code>HttpSessionCsrfTokenRepository</code> reads the token from a session (whether in-memory, cache, or database). If you need to access the session attribute directly, please first configure the session attribute name using <code>HttpSessionCsrfTokenRepository#setSessionAttributeName</code>.</p>
</div>
<div class="paragraph">
<p>You can specify the default configuration explicitly using the following configuration:</p>
</div>
<div id="csrf-token-repository-httpsession-configuration" class="openblock tabs is-sync is-loading">
<div class="title">Configure <code>HttpSessionCsrfTokenRepository</code></div>
<div class="content">
<div class="ulist tablist">
<ul>
<li id="csrf_token_repository_httpsession_configuration_java" class="tab">
<p>Java</p>
</li>
<li id="csrf_token_repository_httpsession_configuration_kotlin" class="tab">
<p>Kotlin</p>
</li>
<li id="csrf_token_repository_httpsession_configuration_xml" class="tab">
<p>XML</p>
</li>
</ul>
</div>
<div id="csrf_token_repository_httpsession_configuration_java--panel" class="tabpanel" aria-labelledby="csrf_token_repository_httpsession_configuration_java">
<div class="listingblock primary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			// ...
			.csrf((csrf) -&gt; csrf
				.csrfTokenRepository(new HttpSessionCsrfTokenRepository())
			);
		return http.build();
	}
}</code></pre>
</div>
</div>
</div>
<div id="csrf_token_repository_httpsession_configuration_kotlin--panel" class="tabpanel" aria-labelledby="csrf_token_repository_httpsession_configuration_kotlin">
<div class="listingblock secondary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-kotlin hljs" data-lang="kotlin"><span class="fold-block is-hidden-folded">import org.springframework.security.config.annotation.web.invoke

</span><span class="fold-block">@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    open fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            // ...
            csrf {
                csrfTokenRepository = HttpSessionCsrfTokenRepository()
            }
        }
        return http.build()
    }
}</span></code></pre>
</div>
</div>
</div>
<div id="csrf_token_repository_httpsession_configuration_xml--panel" class="tabpanel" aria-labelledby="csrf_token_repository_httpsession_configuration_xml">
<div class="listingblock secondary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-xml hljs" data-lang="xml">&lt;http&gt;
	&lt;!-- ... --&gt;
	&lt;csrf token-repository-ref="tokenRepository"/&gt;
&lt;/http&gt;
&lt;b:bean id="tokenRepository"
	class="org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository"/&gt;</code></pre>
</div>
</div>
</div>
</div>
</div>
</div>
<div class="sect2">
<h3 id="csrf-token-repository-cookie"><a class="anchor" href="#csrf-token-repository-cookie"></a>Using the <code>CookieCsrfTokenRepository</code></h3>
<div class="paragraph">
<p>You can persist the <code>CsrfToken</code> in a cookie to <a href="#csrf-integration-javascript">support a JavaScript-based application</a> using the <a href="../../api/java/org/springframework/security/web/csrf/CookieCsrfTokenRepository.html" class="xref attachment apiref"><code>CookieCsrfTokenRepository</code></a>.</p>
</div>
<div class="paragraph">
<p>The <code>CookieCsrfTokenRepository</code> writes to a cookie named <code>XSRF-TOKEN</code> and reads it from an HTTP request header named <code>X-XSRF-TOKEN</code> or the request parameter <code>_csrf</code> by default.
These defaults come from Angular and its predecessor <a href="https://docs.angularjs.org/api/ng/service/$http#cross-site-request-forgery-xsrf-protection">AngularJS</a>.</p>
</div>
<div class="admonitionblock tip">
<table>
<tr>
<td class="icon">
<i class="fa icon-tip" title="Tip"></i>
</td>
<td class="content">
<div class="paragraph">
<p>See the <a href="https://angular.dev/best-practices/security#httpclient-xsrf-csrf-security">HttpClient XSRF/CSRF security</a> and the <a href="https://angular.dev/api/common/http/withXsrfConfiguration">withXsrfConfiguration</a> for more recent information on this topic.</p>
</div>
</td>
</tr>
</table>
</div>
<div class="paragraph">
<p>You can configure the <code>CookieCsrfTokenRepository</code> using the following configuration:</p>
</div>
<div id="csrf-token-repository-cookie-configuration" class="openblock tabs is-sync is-loading">
<div class="title">Configure <code>CookieCsrfTokenRepository</code></div>
<div class="content">
<div class="ulist tablist">
<ul>
<li id="csrf_token_repository_cookie_configuration_java" class="tab">
<p>Java</p>
</li>
<li id="csrf_token_repository_cookie_configuration_kotlin" class="tab">
<p>Kotlin</p>
</li>
<li id="csrf_token_repository_cookie_configuration_xml" class="tab">
<p>XML</p>
</li>
</ul>
</div>
<div id="csrf_token_repository_cookie_configuration_java--panel" class="tabpanel" aria-labelledby="csrf_token_repository_cookie_configuration_java">
<div class="listingblock primary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			// ...
			.csrf((csrf) -&gt; csrf
				.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
			);
		return http.build();
	}
}</code></pre>
</div>
</div>
</div>
<div id="csrf_token_repository_cookie_configuration_kotlin--panel" class="tabpanel" aria-labelledby="csrf_token_repository_cookie_configuration_kotlin">
<div class="listingblock secondary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-kotlin hljs" data-lang="kotlin"><span class="fold-block is-hidden-folded">import org.springframework.security.config.annotation.web.invoke

</span><span class="fold-block">@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    open fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            // ...
            csrf {
                csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse()
            }
        }
        return http.build()
    }
}</span></code></pre>
</div>
</div>
</div>
<div id="csrf_token_repository_cookie_configuration_xml--panel" class="tabpanel" aria-labelledby="csrf_token_repository_cookie_configuration_xml">
<div class="listingblock secondary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-xml hljs" data-lang="xml">&lt;http&gt;
	&lt;!-- ... --&gt;
	&lt;csrf token-repository-ref="tokenRepository"/&gt;
&lt;/http&gt;
&lt;b:bean id="tokenRepository"
	class="org.springframework.security.web.csrf.CookieCsrfTokenRepository"
	p:cookieHttpOnly="false"/&gt;</code></pre>
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
<p>The example explicitly sets <code>HttpOnly</code> to <code>false</code>.
This is necessary to let JavaScript frameworks (such as Angular) read it.
If you do not need the ability to read the cookie with JavaScript directly, we <em>recommend</em> omitting <code>HttpOnly</code> (by using <code>new CookieCsrfTokenRepository()</code> instead) to improve security.</p>
</div>
</td>
</tr>
</table>
</div>
</div>
<div class="sect2">
<h3 id="csrf-token-repository-custom"><a class="anchor" href="#csrf-token-repository-custom"></a>Customizing the <code>CsrfTokenRepository</code></h3>
<div class="paragraph">
<p>There can be cases where you want to implement a custom <a href="../../api/java/org/springframework/security/web/csrf/CsrfTokenRepository.html" class="xref attachment apiref"><code>CsrfTokenRepository</code></a>.</p>
</div>
<div class="paragraph">
<p>Once you&#8217;ve implemented the <code>CsrfTokenRepository</code> interface, you can configure Spring Security to use it with the following configuration:</p>
</div>
<div id="csrf-token-repository-custom-configuration" class="openblock tabs is-sync is-loading">
<div class="title">Configure Custom <code>CsrfTokenRepository</code></div>
<div class="content">
<div class="ulist tablist">
<ul>
<li id="csrf_token_repository_custom_configuration_java" class="tab">
<p>Java</p>
</li>
<li id="csrf_token_repository_custom_configuration_kotlin" class="tab">
<p>Kotlin</p>
</li>
<li id="csrf_token_repository_custom_configuration_xml" class="tab">
<p>XML</p>
</li>
</ul>
</div>
<div id="csrf_token_repository_custom_configuration_java--panel" class="tabpanel" aria-labelledby="csrf_token_repository_custom_configuration_java">
<div class="listingblock primary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			// ...
			.csrf((csrf) -&gt; csrf
				.csrfTokenRepository(new CustomCsrfTokenRepository())
			);
		return http.build();
	}
}</code></pre>
</div>
</div>
</div>
<div id="csrf_token_repository_custom_configuration_kotlin--panel" class="tabpanel" aria-labelledby="csrf_token_repository_custom_configuration_kotlin">
<div class="listingblock secondary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-kotlin hljs" data-lang="kotlin"><span class="fold-block is-hidden-folded">import org.springframework.security.config.annotation.web.invoke

</span><span class="fold-block">@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    open fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            // ...
            csrf {
                csrfTokenRepository = CustomCsrfTokenRepository()
            }
        }
        return http.build()
    }
}</span></code></pre>
</div>
</div>
</div>
<div id="csrf_token_repository_custom_configuration_xml--panel" class="tabpanel" aria-labelledby="csrf_token_repository_custom_configuration_xml">
<div class="listingblock secondary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-xml hljs" data-lang="xml">&lt;http&gt;
	&lt;!-- ... --&gt;
	&lt;csrf token-repository-ref="tokenRepository"/&gt;
&lt;/http&gt;
&lt;b:bean id="tokenRepository"
	class="example.CustomCsrfTokenRepository"/&gt;</code></pre>
</div>
</div>
</div>
</div>
</div>
</div>
</div>
</div>
<div class="sect1">
<h2 id="csrf-token-request-handler"><a class="anchor" href="#csrf-token-request-handler"></a>Handling the <code>CsrfToken</code></h2>
<div class="sectionbody">
<div class="paragraph">
<p>The <code>CsrfToken</code> is made available to an application using a <code>CsrfTokenRequestHandler</code>.
This component is also responsible for resolving the <code>CsrfToken</code> from HTTP headers or request parameters.</p>
</div>
<div class="paragraph">
<p>By default, the <a href="#csrf-token-request-handler-breach"><code>XorCsrfTokenRequestAttributeHandler</code></a> is used for providing <a href="https://en.wikipedia.org/wiki/BREACH">BREACH</a> protection of the <code>CsrfToken</code>.
Spring Security also provides the <a href="#csrf-token-request-handler-plain"><code>CsrfTokenRequestAttributeHandler</code></a> for opting out of BREACH protection.
You can also specify <a href="#csrf-token-request-handler-custom">your own implementation</a> to customize the strategy for handling and resolving tokens.</p>
</div>
<div class="sect2">
<h3 id="csrf-token-request-handler-breach"><a class="anchor" href="#csrf-token-request-handler-breach"></a>Using the <code>XorCsrfTokenRequestAttributeHandler</code> (BREACH)</h3>
<div class="paragraph">
<p>The <code>XorCsrfTokenRequestAttributeHandler</code> makes the <code>CsrfToken</code> available as an <code>HttpServletRequest</code> attribute called <code>_csrf</code>, and additionally provides protection for <a href="https://en.wikipedia.org/wiki/BREACH">BREACH</a>.</p>
</div>
<div class="admonitionblock note">
<table>
<tr>
<td class="icon">
<i class="fa icon-note" title="Note"></i>
</td>
<td class="content">
<div class="paragraph">
<p>The <code>CsrfToken</code> is also made available as a request attribute using the name <code>CsrfToken.class.getName()</code>.
This name is not configurable, but the name <code>_csrf</code> can be changed using <code>XorCsrfTokenRequestAttributeHandler#setCsrfRequestAttributeName</code>.</p>
</div>
</td>
</tr>
</table>
</div>
<div class="paragraph">
<p>This implementation also resolves the token value from the request as either a request header (one of <a href="#csrf-token-repository-httpsession"><code>X-CSRF-TOKEN</code></a> or <a href="#csrf-token-repository-cookie"><code>X-XSRF-TOKEN</code></a> by default) or a request parameter (<code>_csrf</code> by default).</p>
</div>
<div class="admonitionblock note">
<table>
<tr>
<td class="icon">
<i class="fa icon-note" title="Note"></i>
</td>
<td class="content">
<div class="paragraph">
<p>BREACH protection is provided by encoding randomness into the CSRF token value to ensure the returned <code>CsrfToken</code> changes on every request.
When the token is later resolved as a header value or request parameter, it is decoded to obtain the raw token which is then compared to the <a href="#csrf-token-repository">persisted <code>CsrfToken</code></a>.</p>
</div>
</td>
</tr>
</table>
</div>
<div class="paragraph">
<p>Spring Security protects the CSRF token from a BREACH attack by default, so no additional code is necessary.
You can specify the default configuration explicitly using the following configuration:</p>
</div>
<div id="csrf-token-request-handler-breach-configuration" class="openblock tabs is-sync is-loading">
<div class="title">Configure BREACH protection</div>
<div class="content">
<div class="ulist tablist">
<ul>
<li id="csrf_token_request_handler_breach_configuration_java" class="tab">
<p>Java</p>
</li>
<li id="csrf_token_request_handler_breach_configuration_kotlin" class="tab">
<p>Kotlin</p>
</li>
<li id="csrf_token_request_handler_breach_configuration_xml" class="tab">
<p>XML</p>
</li>
</ul>
</div>
<div id="csrf_token_request_handler_breach_configuration_java--panel" class="tabpanel" aria-labelledby="csrf_token_request_handler_breach_configuration_java">
<div class="listingblock primary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			// ...
			.csrf((csrf) -&gt; csrf
				.csrfTokenRequestHandler(new XorCsrfTokenRequestAttributeHandler())
			);
		return http.build();
	}
}</code></pre>
</div>
</div>
</div>
<div id="csrf_token_request_handler_breach_configuration_kotlin--panel" class="tabpanel" aria-labelledby="csrf_token_request_handler_breach_configuration_kotlin">
<div class="listingblock secondary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-kotlin hljs" data-lang="kotlin"><span class="fold-block is-hidden-folded">import org.springframework.security.config.annotation.web.invoke

</span><span class="fold-block">@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    open fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            // ...
            csrf {
                csrfTokenRequestHandler = XorCsrfTokenRequestAttributeHandler()
            }
        }
        return http.build()
    }
}</span></code></pre>
</div>
</div>
</div>
<div id="csrf_token_request_handler_breach_configuration_xml--panel" class="tabpanel" aria-labelledby="csrf_token_request_handler_breach_configuration_xml">
<div class="listingblock secondary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-xml hljs" data-lang="xml">&lt;http&gt;
	&lt;!-- ... --&gt;
	&lt;csrf request-handler-ref="requestHandler"/&gt;
&lt;/http&gt;
&lt;b:bean id="requestHandler"
	class="org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler"/&gt;</code></pre>
</div>
</div>
</div>
</div>
</div>
</div>
<div class="sect2">
<h3 id="csrf-token-request-handler-plain"><a class="anchor" href="#csrf-token-request-handler-plain"></a>Using the <code>CsrfTokenRequestAttributeHandler</code></h3>
<div class="paragraph">
<p>The <code>CsrfTokenRequestAttributeHandler</code> makes the <code>CsrfToken</code> available as an <code>HttpServletRequest</code> attribute called <code>_csrf</code>.</p>
</div>
<div class="admonitionblock note">
<table>
<tr>
<td class="icon">
<i class="fa icon-note" title="Note"></i>
</td>
<td class="content">
<div class="paragraph">
<p>The <code>CsrfToken</code> is also made available as a request attribute using the name <code>CsrfToken.class.getName()</code>.
This name is not configurable, but the name <code>_csrf</code> can be changed using <code>CsrfTokenRequestAttributeHandler#setCsrfRequestAttributeName</code>.</p>
</div>
</td>
</tr>
</table>
</div>
<div class="paragraph">
<p>This implementation also resolves the token value from the request as either a request header (one of <a href="#csrf-token-repository-httpsession"><code>X-CSRF-TOKEN</code></a> or <a href="#csrf-token-repository-cookie"><code>X-XSRF-TOKEN</code></a> by default) or a request parameter (<code>_csrf</code> by default).</p>
</div>
<div id="csrf-token-request-handler-opt-out-of-breach" class="paragraph">
<p>The primary use of <code>CsrfTokenRequestAttributeHandler</code> is to opt-out of BREACH protection of the <code>CsrfToken</code>, which can be configured using the following configuration:</p>
</div>
<div id="_tabs_6" class="openblock tabs is-sync is-loading">
<div class="title">Opt-out of BREACH protection</div>
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
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			// ...
			.csrf((csrf) -&gt; csrf
				.csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
			);
		return http.build();
	}
}</code></pre>
</div>
</div>
</div>
<div id="_tabs_6_kotlin--panel" class="tabpanel" aria-labelledby="_tabs_6_kotlin">
<div class="listingblock secondary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-kotlin hljs" data-lang="kotlin"><span class="fold-block is-hidden-folded">import org.springframework.security.config.annotation.web.invoke

</span><span class="fold-block">@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    open fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            // ...
            csrf {
                csrfTokenRequestHandler = CsrfTokenRequestAttributeHandler()
            }
        }
        return http.build()
    }
}</span></code></pre>
</div>
</div>
</div>
<div id="_tabs_6_xml--panel" class="tabpanel" aria-labelledby="_tabs_6_xml">
<div class="listingblock secondary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-xml hljs" data-lang="xml">&lt;http&gt;
	&lt;!-- ... --&gt;
	&lt;csrf request-handler-ref="requestHandler"/&gt;
&lt;/http&gt;
&lt;b:bean id="requestHandler"
	class="org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler"/&gt;</code></pre>
</div>
</div>
</div>
</div>
</div>
</div>
<div class="sect2">
<h3 id="csrf-token-request-handler-custom"><a class="anchor" href="#csrf-token-request-handler-custom"></a>Customizing the <code>CsrfTokenRequestHandler</code></h3>
<div class="paragraph">
<p>You can implement the <code>CsrfTokenRequestHandler</code> interface to customize the strategy for handling and resolving tokens.</p>
</div>
<div class="admonitionblock tip">
<table>
<tr>
<td class="icon">
<i class="fa icon-tip" title="Tip"></i>
</td>
<td class="content">
<div class="paragraph">
<p>The <code>CsrfTokenRequestHandler</code> interface is a <code>@FunctionalInterface</code> that can be implemented using a lambda expression to customize request handling.
You will need to implement the full interface to customize how tokens are resolved from the request.
See <a href="#csrf-integration-javascript-spa-configuration">Configure CSRF for Single-Page Application</a> for an example that uses delegation to implement a custom strategy for handling and resolving tokens.</p>
</div>
</td>
</tr>
</table>
</div>
<div class="paragraph">
<p>Once you&#8217;ve implemented the <code>CsrfTokenRequestHandler</code> interface, you can configure Spring Security to use it with the following configuration:</p>
</div>
<div id="csrf-token-request-handler-custom-configuration" class="openblock tabs is-sync is-loading">
<div class="title">Configure Custom <code>CsrfTokenRequestHandler</code></div>
<div class="content">
<div class="ulist tablist">
<ul>
<li id="csrf_token_request_handler_custom_configuration_java" class="tab">
<p>Java</p>
</li>
<li id="csrf_token_request_handler_custom_configuration_kotlin" class="tab">
<p>Kotlin</p>
</li>
<li id="csrf_token_request_handler_custom_configuration_xml" class="tab">
<p>XML</p>
</li>
</ul>
</div>
<div id="csrf_token_request_handler_custom_configuration_java--panel" class="tabpanel" aria-labelledby="csrf_token_request_handler_custom_configuration_java">
<div class="listingblock primary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			// ...
			.csrf((csrf) -&gt; csrf
				.csrfTokenRequestHandler(new CustomCsrfTokenRequestHandler())
			);
		return http.build();
	}
}</code></pre>
</div>
</div>
</div>
<div id="csrf_token_request_handler_custom_configuration_kotlin--panel" class="tabpanel" aria-labelledby="csrf_token_request_handler_custom_configuration_kotlin">
<div class="listingblock secondary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-kotlin hljs" data-lang="kotlin"><span class="fold-block is-hidden-folded">import org.springframework.security.config.annotation.web.invoke

</span><span class="fold-block">@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    open fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            // ...
            csrf {
                csrfTokenRequestHandler = CustomCsrfTokenRequestHandler()
            }
        }
        return http.build()
    }
}</span></code></pre>
</div>
</div>
</div>
<div id="csrf_token_request_handler_custom_configuration_xml--panel" class="tabpanel" aria-labelledby="csrf_token_request_handler_custom_configuration_xml">
<div class="listingblock secondary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-xml hljs" data-lang="xml">&lt;http&gt;
	&lt;!-- ... --&gt;
	&lt;csrf request-handler-ref="requestHandler"/&gt;
&lt;/http&gt;
&lt;b:bean id="requestHandler"
	class="example.CustomCsrfTokenRequestHandler"/&gt;</code></pre>
</div>
</div>
</div>
</div>
</div>
</div>
</div>
</div>
<div class="sect1">
<h2 id="deferred-csrf-token"><a class="anchor" href="#deferred-csrf-token"></a>Deferred Loading of the <code>CsrfToken</code></h2>
<div class="sectionbody">
<div class="paragraph">
<p>By default, Spring Security defers loading of the <code>CsrfToken</code> until it is needed.</p>
</div>
<div class="admonitionblock note">
<table>
<tr>
<td class="icon">
<i class="fa icon-note" title="Note"></i>
</td>
<td class="content">
<div class="paragraph">
<p>The <code>CsrfToken</code> is needed whenever a request is made with an <a href="../../features/exploits/csrf.html#csrf-protection-read-only" class="xref page">unsafe HTTP method</a>, such as a POST.
Additionally, it is needed by any request that renders the token to the response, such as a web page with a <code>&lt;form&gt;</code> tag that includes a hidden <code>&lt;input&gt;</code> for the CSRF token.</p>
</div>
</td>
</tr>
</table>
</div>
<div class="paragraph">
<p>Because Spring Security also stores the <code>CsrfToken</code> in the <code>HttpSession</code> by default, deferred CSRF tokens can improve performance by not requiring the session to be loaded on every request.</p>
</div>
<div id="deferred-csrf-token-opt-out" class="paragraph">
<p>In the event that you want to opt-out of deferred tokens and cause the <code>CsrfToken</code> to be loaded on every request, you can do so with the following configuration:</p>
</div>
<div id="deferred-csrf-token-opt-out-configuration" class="openblock tabs is-sync is-loading">
<div class="title">Opt-out of Deferred CSRF Tokens</div>
<div class="content">
<div class="ulist tablist">
<ul>
<li id="deferred_csrf_token_opt_out_configuration_java" class="tab">
<p>Java</p>
</li>
<li id="deferred_csrf_token_opt_out_configuration_kotlin" class="tab">
<p>Kotlin</p>
</li>
<li id="deferred_csrf_token_opt_out_configuration_xml" class="tab">
<p>XML</p>
</li>
</ul>
</div>
<div id="deferred_csrf_token_opt_out_configuration_java--panel" class="tabpanel" aria-labelledby="deferred_csrf_token_opt_out_configuration_java">
<div class="listingblock primary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		XorCsrfTokenRequestAttributeHandler requestHandler = new XorCsrfTokenRequestAttributeHandler();
		// set the name of the attribute the CsrfToken will be populated on
		requestHandler.setCsrfRequestAttributeName(null);
		http
			// ...
			.csrf((csrf) -&gt; csrf
				.csrfTokenRequestHandler(requestHandler)
			);
		return http.build();
	}
}</code></pre>
</div>
</div>
</div>
<div id="deferred_csrf_token_opt_out_configuration_kotlin--panel" class="tabpanel" aria-labelledby="deferred_csrf_token_opt_out_configuration_kotlin">
<div class="listingblock secondary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-kotlin hljs" data-lang="kotlin"><span class="fold-block is-hidden-folded">import org.springframework.security.config.annotation.web.invoke

</span><span class="fold-block">@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    open fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        val requestHandler = XorCsrfTokenRequestAttributeHandler()
        // set the name of the attribute the CsrfToken will be populated on
        requestHandler.setCsrfRequestAttributeName(null)
        http {
            // ...
            csrf {
                csrfTokenRequestHandler = requestHandler
            }
        }
        return http.build()
    }
}</span></code></pre>
</div>
</div>
</div>
<div id="deferred_csrf_token_opt_out_configuration_xml--panel" class="tabpanel" aria-labelledby="deferred_csrf_token_opt_out_configuration_xml">
<div class="listingblock secondary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-xml hljs" data-lang="xml">&lt;http&gt;
	&lt;!-- ... --&gt;
	&lt;csrf request-handler-ref="requestHandler"/&gt;
&lt;/http&gt;
&lt;b:bean id="requestHandler"
	class="org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler"&gt;
	&lt;b:property name="csrfRequestAttributeName"&gt;
		&lt;b:null/&gt;
	&lt;/b:property&gt;
&lt;/b:bean&gt;</code></pre>
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
<p>By setting the <code>csrfRequestAttributeName</code> to <code>null</code>, the <code>CsrfToken</code> must first be loaded to determine what attribute name to use.
This causes the <code>CsrfToken</code> to be loaded on every request.</p>
</div>
</td>
</tr>
</table>
</div>
</div>
</div>
<div class="sect1">
<h2 id="csrf-integration"><a class="anchor" href="#csrf-integration"></a>Integrating with CSRF Protection</h2>
<div class="sectionbody">
<div class="paragraph">
<p>For the <a href="../../features/exploits/csrf.html#csrf-protection-stp" class="xref page">synchronizer token pattern</a> to protect against CSRF attacks, we must include the actual CSRF token in the HTTP request.
This must be included in a part of the request (a form parameter, an HTTP header, or other part) that is not automatically included in the HTTP request by the browser.</p>
</div>
<div class="paragraph">
<p>The following sections describe the various ways a frontend or client application can integrate with a CSRF-protected backend application:</p>
</div>
<div class="ulist">
<ul>
<li>
<p><a href="#csrf-integration-form">HTML Forms</a></p>
</li>
<li>
<p><a href="#csrf-integration-javascript">JavaScript Applications</a></p>
</li>
<li>
<p><a href="#csrf-integration-mobile">Mobile Applications</a></p>
</li>
</ul>
</div>
<div class="sect2">
<h3 id="csrf-integration-form"><a class="anchor" href="#csrf-integration-form"></a>HTML Forms</h3>
<div class="paragraph">
<p>To submit an HTML form, the CSRF token must be included in the form as a hidden input.
For example, the rendered HTML might look like:</p>
</div>
<div class="listingblock">
<div class="title">CSRF Token in HTML Form</div>
<div class="content">
<pre class="highlightjs highlight"><code class="language-html hljs" data-lang="html">&lt;input type="hidden"
	name="_csrf"
	value="4bfd1575-3ad1-4d21-96c7-4ef2d9f86721"/&gt;</code></pre>
</div>
</div>
<div class="paragraph">
<p>The following view technologies automatically include the actual CSRF token in a form that has an unsafe HTTP method, such as a POST:</p>
</div>
<div class="ulist">
<ul>
<li>
<p><a href="https://docs.spring.io/spring-framework/reference/7.0.9/web/webmvc-view/mvc-jsp.html#mvc-view-jsp-formtaglib">Spring’s form tag library</a></p>
</li>
<li>
<p><a href="https://www.thymeleaf.org/doc/tutorials/3.1/thymeleafspring.html#integration-with-requestdatavalueprocessor">Thymeleaf</a></p>
</li>
<li>
<p>Any other view technology that integrates with <a href="https://docs.spring.io/spring-framework/docs/7.0.9/javadoc-api/org/springframework/web/servlet/support/RequestDataValueProcessor.html"><code>RequestDataValueProcessor</code></a> (via <a href="../../api/java/org/springframework/security/web/servlet/support/csrf/CsrfRequestDataValueProcessor.html" class="xref attachment apiref"><code>CsrfRequestDataValueProcessor</code></a>)</p>
</li>
<li>
<p>You can also include the token yourself via the <a href="../integrations/jsp-taglibs.html#taglibs-csrfinput" class="xref page">csrfInput</a> tag</p>
</li>
</ul>
</div>
<div class="paragraph">
<p>If these options are not available, you can take advantage of the fact that the <code>CsrfToken</code> is exposed as an <a href="#csrf-token-request-handler"><code>HttpServletRequest</code> attribute named <code>_csrf</code></a>.
The following example does this with a JSP:</p>
</div>
<div class="listingblock">
<div class="title">CSRF Token in HTML Form with Request Attribute</div>
<div class="content">
<pre class="highlightjs highlight"><code class="language-xml hljs" data-lang="xml">&lt;c:url var="logoutUrl" value="/logout"/&gt;
&lt;form action="${logoutUrl}"
	method="post"&gt;
&lt;input type="submit"
	value="Log out" /&gt;
&lt;input type="hidden"
	name="${_csrf.parameterName}"
	value="${_csrf.token}"/&gt;
&lt;/form&gt;</code></pre>
</div>
</div>
</div>
<div class="sect2">
<h3 id="csrf-integration-javascript"><a class="anchor" href="#csrf-integration-javascript"></a>JavaScript Applications</h3>
<div class="paragraph">
<p>JavaScript applications typically use JSON instead of HTML.
If you use JSON, you can submit the CSRF token within an HTTP request header instead of a request parameter.</p>
</div>
<div class="paragraph">
<p>In order to obtain the CSRF token, you can configure Spring Security to store the expected CSRF token <a href="#csrf-token-repository-cookie">in a cookie</a>.
By storing the expected token in a cookie, JavaScript frameworks such as <a href="https://angular.io/api/common/http/HttpClientXsrfModule">Angular</a> can automatically include the actual CSRF token as an HTTP request header.</p>
</div>
<div class="admonitionblock tip">
<table>
<tr>
<td class="icon">
<i class="fa icon-tip" title="Tip"></i>
</td>
<td class="content">
<div class="paragraph">
<p>There are special considerations for BREACH protection and deferred tokens when integrating a single-page application (SPA) with Spring Security&#8217;s CSRF protection.
A full configuration example is provided in the <a href="#csrf-integration-javascript-spa">next section</a>.</p>
</div>
</td>
</tr>
</table>
</div>
<div class="paragraph">
<p>You can read about different types of JavaScript applications in the following sections:</p>
</div>
<div class="ulist">
<ul>
<li>
<p><a href="#csrf-integration-javascript-spa">Single-Page Applications</a></p>
</li>
<li>
<p><a href="#csrf-integration-javascript-mpa">Multi-Page Applications</a></p>
</li>
<li>
<p><a href="#csrf-integration-javascript-other">Other JavaScript Applications</a></p>
</li>
</ul>
</div>
<div class="sect3">
<h4 id="csrf-integration-javascript-spa"><a class="anchor" href="#csrf-integration-javascript-spa"></a>Single-Page Applications</h4>
<div class="paragraph">
<p>There are special considerations for integrating a single-page application (SPA) with Spring Security&#8217;s CSRF protection.</p>
</div>
<div class="paragraph">
<p>Recall that Spring Security provides <a href="#csrf-token-request-handler-breach">BREACH protection of the <code>CsrfToken</code></a> by default.
When storing the expected CSRF token <a href="#csrf-token-repository-cookie">in a cookie</a>, JavaScript applications will only have access to the plain token value and <em>will not</em> have access to the encoded value.
A <a href="#csrf-token-request-handler-custom">customized request handler</a> for resolving the actual token value will need to be provided.</p>
</div>
<div class="paragraph">
<p>In addition, the cookie storing the CSRF token will be cleared upon authentication success and logout success.
Spring Security defers loading a new CSRF token by default, and additional work is required to return a fresh cookie.</p>
</div>
<div class="admonitionblock note">
<table>
<tr>
<td class="icon">
<i class="fa icon-note" title="Note"></i>
</td>
<td class="content">
<div class="paragraph">
<p>Refreshing the token after authentication success and logout success is required because the <a href="../../api/java/org/springframework/security/web/csrf/CsrfAuthenticationStrategy.html" class="xref attachment apiref"><code>CsrfAuthenticationStrategy</code></a> and <a href="../../api/java/org/springframework/security/web/csrf/CsrfLogoutHandler.html" class="xref attachment apiref"><code>CsrfLogoutHandler</code></a> will clear the previous token.
The client application will not be able to perform an unsafe HTTP request, such as a POST, without obtaining a fresh token.</p>
</div>
</td>
</tr>
</table>
</div>
<div class="paragraph">
<p>In order to easily integrate a single-page application with Spring Security, the following configuration can be used:</p>
</div>
<div id="csrf-integration-javascript-spa-configuration" class="openblock tabs is-sync is-loading">
<div class="title">Configure CSRF for Single-Page Application</div>
<div class="content">
<div class="ulist tablist">
<ul>
<li id="csrf_integration_javascript_spa_configuration_java" class="tab">
<p>Java</p>
</li>
<li id="csrf_integration_javascript_spa_configuration_kotlin" class="tab">
<p>Kotlin</p>
</li>
<li id="csrf_integration_javascript_spa_configuration_xml" class="tab">
<p>XML</p>
</li>
</ul>
</div>
<div id="csrf_integration_javascript_spa_configuration_java--panel" class="tabpanel" aria-labelledby="csrf_integration_javascript_spa_configuration_java">
<div class="listingblock primary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			// ...
			.csrf((csrf) -&gt; csrf.spa());
		return http.build();
	}
}</code></pre>
</div>
</div>
</div>
<div id="csrf_integration_javascript_spa_configuration_kotlin--panel" class="tabpanel" aria-labelledby="csrf_integration_javascript_spa_configuration_kotlin">
<div class="listingblock secondary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-kotlin hljs" data-lang="kotlin"><span class="fold-block is-hidden-folded">import org.springframework.security.config.annotation.web.invoke

</span><span class="fold-block">@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    open fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            // ...
            csrf {
                spa()
            }
        }
        return http.build()
    }
}</span></code></pre>
</div>
</div>
</div>
<div id="csrf_integration_javascript_spa_configuration_xml--panel" class="tabpanel" aria-labelledby="csrf_integration_javascript_spa_configuration_xml">
<div class="listingblock secondary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-xml hljs" data-lang="xml">&lt;http&gt;
	&lt;!-- ... --&gt;
	&lt;csrf&gt;
        &lt;spa /&gt;
    &lt;/csrf&gt;
&lt;/http&gt;</code></pre>
</div>
</div>
</div>
</div>
</div>
</div>
<div class="sect3">
<h4 id="csrf-integration-javascript-mpa"><a class="anchor" href="#csrf-integration-javascript-mpa"></a>Multi-Page Applications</h4>
<div class="paragraph">
<p>For multi-page applications where JavaScript is loaded on each page, an alternative to exposing the CSRF token <a href="#csrf-token-repository-cookie">in a cookie</a> is to include the CSRF token within your <code>meta</code> tags.
The HTML might look something like this:</p>
</div>
<div class="listingblock">
<div class="title">CSRF Token in HTML Meta Tag</div>
<div class="content">
<pre class="highlightjs highlight"><code class="language-html hljs" data-lang="html">&lt;html&gt;
&lt;head&gt;
	&lt;meta name="_csrf" content="4bfd1575-3ad1-4d21-96c7-4ef2d9f86721"/&gt;
	&lt;meta name="_csrf_header" content="X-CSRF-TOKEN"/&gt;
	&lt;!-- ... --&gt;
&lt;/head&gt;
&lt;!-- ... --&gt;
&lt;/html&gt;</code></pre>
</div>
</div>
<div class="paragraph">
<p>In order to include the CSRF token in the request, you can take advantage of the fact that the <code>CsrfToken</code> is exposed as an <a href="#csrf-token-request-handler"><code>HttpServletRequest</code> attribute named <code>_csrf</code></a>.
The following example does this with a JSP:</p>
</div>
<div class="listingblock">
<div class="title">CSRF Token in HTML Meta Tag with Request Attribute</div>
<div class="content">
<pre class="highlightjs highlight"><code class="language-html hljs" data-lang="html">&lt;html&gt;
&lt;head&gt;
	&lt;meta name="_csrf" content="${_csrf.token}"/&gt;
	&lt;!-- default header name is X-CSRF-TOKEN --&gt;
	&lt;meta name="_csrf_header" content="${_csrf.headerName}"/&gt;
	&lt;!-- ... --&gt;
&lt;/head&gt;
&lt;!-- ... --&gt;
&lt;/html&gt;</code></pre>
</div>
</div>
<div class="paragraph">
<p>Once the meta tags contain the CSRF token, the JavaScript code can read the meta tags and include the CSRF token as a header.
If you use jQuery, you can do this with the following code:</p>
</div>
<div class="listingblock">
<div class="title">Include CSRF Token in AJAX Request</div>
<div class="content">
<pre class="highlightjs highlight"><code class="language-javascript hljs" data-lang="javascript">$(function () {
	var token = $("meta[name='_csrf']").attr("content");
	var header = $("meta[name='_csrf_header']").attr("content");
	$(document).ajaxSend(function(e, xhr, options) {
		xhr.setRequestHeader(header, token);
	});
});</code></pre>
</div>
</div>
</div>
<div class="sect3">
<h4 id="csrf-integration-javascript-other"><a class="anchor" href="#csrf-integration-javascript-other"></a>Other JavaScript Applications</h4>
<div class="paragraph">
<p>Another option for JavaScript applications is to include the CSRF token in an HTTP response header.</p>
</div>
<div class="paragraph">
<p>One way to achieve this is through the use of a <code>@ControllerAdvice</code> with the <a href="../integrations/mvc.html#mvc-csrf-resolver" class="xref page"><code>CsrfTokenArgumentResolver</code></a>.
The following is an example of <code>@ControllerAdvice</code> that applies to all controller endpoints in the application:</p>
</div>
<div id="controller-advice" class="openblock tabs is-sync is-loading">
<div class="title">CSRF Token in HTTP Response Header</div>
<div class="content">
<div class="ulist tablist">
<ul>
<li id="controller_advice_java" class="tab">
<p>Java</p>
</li>
<li id="controller_advice_kotlin" class="tab">
<p>Kotlin</p>
</li>
</ul>
</div>
<div id="controller_advice_java--panel" class="tabpanel" aria-labelledby="controller_advice_java">
<div class="listingblock primary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@ControllerAdvice
public class CsrfControllerAdvice {

	@ModelAttribute
	public void getCsrfToken(HttpServletResponse response, CsrfToken csrfToken) {
		response.setHeader(csrfToken.getHeaderName(), csrfToken.getToken());
	}

}</code></pre>
</div>
</div>
</div>
<div id="controller_advice_kotlin--panel" class="tabpanel" aria-labelledby="controller_advice_kotlin">
<div class="listingblock secondary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-kotlin hljs" data-lang="kotlin">@ControllerAdvice
class CsrfControllerAdvice {

	@ModelAttribute
	fun getCsrfToken(response: HttpServletResponse, csrfToken: CsrfToken) {
		response.setHeader(csrfToken.headerName, csrfToken.token)
	}

}</code></pre>
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
<p>Because this <code>@ControllerAdvice</code> applies to all endpoints in the application, it will cause the CSRF token to be loaded on every request, which can negate the benefits of <a href="#deferred-csrf-token">deferred tokens</a> when using the <a href="#csrf-token-repository-httpsession"><code>HttpSessionCsrfTokenRepository</code></a>.
However, this is not usually an issue when using the <a href="#csrf-token-repository-cookie"><code>CookieCsrfTokenRepository</code></a>.</p>
</div>
</td>
</tr>
</table>
</div>
<div class="admonitionblock note">
<table>
<tr>
<td class="icon">
<i class="fa icon-note" title="Note"></i>
</td>
<td class="content">
<div class="paragraph">
<p>It is important to remember that controller endpoints and controller advice are called <em>after</em> the Spring Security filter chain.
This means that this <code>@ControllerAdvice</code> will only be applied if the request passes through the filter chain to your application.
See the configuration for <a href="#csrf-integration-javascript-spa-configuration">single-page applications</a> for an example of adding a filter to the filter chain for earlier access to the <code>HttpServletResponse</code>.</p>
</div>
</td>
</tr>
</table>
</div>
<div class="paragraph">
<p>The CSRF token will now be available in a response header (<a href="#csrf-token-repository-httpsession"><code>X-CSRF-TOKEN</code></a> or <a href="#csrf-token-repository-cookie"><code>X-XSRF-TOKEN</code></a> by default) for any custom endpoints the controller advice applies to.
Any request to the backend can be used to obtain the token from the response, and a subsequent request can include the token in a request header with the same name.</p>
</div>
</div>
</div>
<div class="sect2">
<h3 id="csrf-integration-mobile"><a class="anchor" href="#csrf-integration-mobile"></a>Mobile Applications</h3>
<div class="paragraph">
<p>Like <a href="#csrf-integration-javascript">JavaScript applications</a>, mobile applications typically use JSON instead of HTML.
A backend application that <em>does not</em> serve browser traffic may choose to <a href="#disable-csrf">disable CSRF</a>.
In that case, no additional work is required.</p>
</div>
<div class="paragraph">
<p>However, a backend application that also serves browser traffic and therefore <em>still requires</em> CSRF protection may continue to store the <code>CsrfToken</code> <a href="#csrf-token-repository-httpsession">in the session</a> instead of <a href="#csrf-token-repository-cookie">in a cookie</a>.</p>
</div>
<div class="paragraph">
<p>In this case, a typical pattern for integrating with the backend is to expose a <code>/csrf</code> endpoint to allow the frontend (mobile or browser client) to request a CSRF token on demand.
The benefit of using this pattern is that the CSRF token <a href="#deferred-csrf-token">can continue to be deferred</a> and only needs to be loaded from the session when a request requires CSRF protection.
The use of a custom endpoint also means the client application can request that a new token be generated on demand (if necessary) by issuing an explicit request.</p>
</div>
<div class="admonitionblock tip">
<table>
<tr>
<td class="icon">
<i class="fa icon-tip" title="Tip"></i>
</td>
<td class="content">
<div class="paragraph">
<p>This pattern can be used for any type of application that requires CSRF protection, not just mobile applications.
While this approach isn&#8217;t typically required in those cases, it is another option for integrating with a CSRF-protected backend.</p>
</div>
</td>
</tr>
</table>
</div>
<div class="paragraph">
<p>The following is an example of the <code>/csrf</code> endpoint that makes use of the <a href="../integrations/mvc.html#mvc-csrf-resolver" class="xref page"><code>CsrfTokenArgumentResolver</code></a>:</p>
</div>
<div id="csrf-endpoint" class="openblock tabs is-sync is-loading">
<div class="title">The <code>/csrf</code> endpoint</div>
<div class="content">
<div class="ulist tablist">
<ul>
<li id="csrf_endpoint_java" class="tab">
<p>Java</p>
</li>
<li id="csrf_endpoint_kotlin" class="tab">
<p>Kotlin</p>
</li>
</ul>
</div>
<div id="csrf_endpoint_java--panel" class="tabpanel" aria-labelledby="csrf_endpoint_java">
<div class="listingblock primary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@RestController
public class CsrfController {

    @GetMapping("/csrf")
    public CsrfToken csrf(CsrfToken csrfToken) {
        return csrfToken;
    }

}</code></pre>
</div>
</div>
</div>
<div id="csrf_endpoint_kotlin--panel" class="tabpanel" aria-labelledby="csrf_endpoint_kotlin">
<div class="listingblock secondary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-kotlin hljs" data-lang="kotlin">@RestController
class CsrfController {

    @GetMapping("/csrf")
    fun csrf(csrfToken: CsrfToken): CsrfToken {
        return csrfToken
    }

}</code></pre>
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
<p>You may consider adding <code>.requestMatchers("/csrf").permitAll()</code> if the endpoint above is required prior to authenticating with the server.</p>
</div>
</td>
</tr>
</table>
</div>
<div class="paragraph">
<p>This endpoint should be called to obtain a CSRF token when the application is launched or initialized (e.g. at load time), and also after authentication success and logout success.</p>
</div>
<div class="admonitionblock note">
<table>
<tr>
<td class="icon">
<i class="fa icon-note" title="Note"></i>
</td>
<td class="content">
<div class="paragraph">
<p>Refreshing the token after authentication success and logout success is required because the <a href="../../api/java/org/springframework/security/web/csrf/CsrfAuthenticationStrategy.html" class="xref attachment apiref"><code>CsrfAuthenticationStrategy</code></a> and <a href="../../api/java/org/springframework/security/web/csrf/CsrfLogoutHandler.html" class="xref attachment apiref"><code>CsrfLogoutHandler</code></a> will clear the previous token.
The client application will not be able to perform an unsafe HTTP request, such as a POST, without obtaining a fresh token.</p>
</div>
</td>
</tr>
</table>
</div>
<div class="paragraph">
<p>Once you&#8217;ve obtained the CSRF token, you will need to include it as an HTTP request header (one of <a href="#csrf-token-repository-httpsession"><code>X-CSRF-TOKEN</code></a> or <a href="#csrf-token-repository-cookie"><code>X-XSRF-TOKEN</code></a> by default) yourself.</p>
</div>
</div>
</div>
</div>
<div class="sect1">
<h2 id="csrf-access-denied-handler"><a class="anchor" href="#csrf-access-denied-handler"></a>Handle <code>AccessDeniedException</code></h2>
<div class="sectionbody">
<div class="paragraph">
<p>To handle an <code>AccessDeniedException</code> such as <code>InvalidCsrfTokenException</code>, you can configure Spring Security to handle these exceptions in any way you like.
For example, you can configure a custom access denied page using the following configuration:</p>
</div>
<div id="csrf-access-denied-handler-configuration" class="openblock tabs is-sync is-loading">
<div class="title">Configure <code>AccessDeniedHandler</code></div>
<div class="content">
<div class="ulist tablist">
<ul>
<li id="csrf_access_denied_handler_configuration_java" class="tab">
<p>Java</p>
</li>
<li id="csrf_access_denied_handler_configuration_kotlin" class="tab">
<p>Kotlin</p>
</li>
<li id="csrf_access_denied_handler_configuration_xml" class="tab">
<p>XML</p>
</li>
</ul>
</div>
<div id="csrf_access_denied_handler_configuration_java--panel" class="tabpanel" aria-labelledby="csrf_access_denied_handler_configuration_java">
<div class="listingblock primary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			// ...
			.exceptionHandling((exceptionHandling) -&gt; exceptionHandling
				.accessDeniedPage("/access-denied")
			);
		return http.build();
	}
}</code></pre>
</div>
</div>
</div>
<div id="csrf_access_denied_handler_configuration_kotlin--panel" class="tabpanel" aria-labelledby="csrf_access_denied_handler_configuration_kotlin">
<div class="listingblock secondary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-kotlin hljs" data-lang="kotlin"><span class="fold-block is-hidden-folded">import org.springframework.security.config.annotation.web.invoke

</span><span class="fold-block">@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    open fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            // ...
            exceptionHandling {
                accessDeniedPage = "/access-denied"
            }
        }
        return http.build()
    }
}</span></code></pre>
</div>
</div>
</div>
<div id="csrf_access_denied_handler_configuration_xml--panel" class="tabpanel" aria-labelledby="csrf_access_denied_handler_configuration_xml">
<div class="listingblock secondary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-xml hljs" data-lang="xml">&lt;http&gt;
	&lt;!-- ... --&gt;
	&lt;access-denied-handler error-page="/access-denied"/&gt;
&lt;/http&gt;</code></pre>
</div>
</div>
</div>
</div>
</div>
</div>
</div>
<div class="sect1">
<h2 id="csrf-testing"><a class="anchor" href="#csrf-testing"></a>CSRF Testing</h2>
<div class="sectionbody">
<div class="paragraph">
<p>You can use Spring Security&#8217;s <a href="../test/mockmvc/setup.html" class="xref page">testing support</a> and <a href="../test/mockmvc/csrf.html" class="xref page"><code>CsrfRequestPostProcessor</code></a> to test CSRF protection, like this:</p>
</div>
<div id="csrf-testing-example" class="openblock tabs is-sync is-loading">
<div class="title">Test CSRF Protection</div>
<div class="content">
<div class="ulist tablist">
<ul>
<li id="csrf_testing_example_java" class="tab">
<p>Java</p>
</li>
<li id="csrf_testing_example_kotlin" class="tab">
<p>Kotlin</p>
</li>
</ul>
</div>
<div id="csrf_testing_example_java--panel" class="tabpanel" aria-labelledby="csrf_testing_example_java">
<div class="listingblock primary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java"><span class="fold-block is-hidden-folded">import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

</span><span class="fold-block">@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SecurityConfig.class)
@WebAppConfiguration
public class CsrfTests {

	private MockMvc mockMvc;

	@BeforeEach
	public void setUp(WebApplicationContext applicationContext) {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext)
			.apply(springSecurity())
			.build();
	}

	@Test
	public void loginWhenValidCsrfTokenThenSuccess() throws Exception {
		this.mockMvc.perform(post("/login").with(csrf())
				.accept(MediaType.TEXT_HTML)
				.param("username", "user")
				.param("password", "password"))
			.andExpect(status().is3xxRedirection())
			.andExpect(header().string(HttpHeaders.LOCATION, "/"));
	}

	@Test
	public void loginWhenInvalidCsrfTokenThenForbidden() throws Exception {
		this.mockMvc.perform(post("/login").with(csrf().useInvalidToken())
				.accept(MediaType.TEXT_HTML)
				.param("username", "user")
				.param("password", "password"))
			.andExpect(status().isForbidden());
	}

	@Test
	public void loginWhenMissingCsrfTokenThenForbidden() throws Exception {
		this.mockMvc.perform(post("/login")
				.accept(MediaType.TEXT_HTML)
				.param("username", "user")
				.param("password", "password"))
			.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser
	public void logoutWhenValidCsrfTokenThenSuccess() throws Exception {
		this.mockMvc.perform(post("/logout").with(csrf())
				.accept(MediaType.TEXT_HTML))
			.andExpect(status().is3xxRedirection())
			.andExpect(header().string(HttpHeaders.LOCATION, "/login?logout"));
	}
}</span></code></pre>
</div>
</div>
</div>
<div id="csrf_testing_example_kotlin--panel" class="tabpanel" aria-labelledby="csrf_testing_example_kotlin">
<div class="listingblock secondary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-kotlin hljs" data-lang="kotlin"><span class="fold-block is-hidden-folded">import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.*
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

</span><span class="fold-block">@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [SecurityConfig::class])
@WebAppConfiguration
class CsrfTests {
	private lateinit var mockMvc: MockMvc

	@BeforeEach
	fun setUp(applicationContext: WebApplicationContext) {
		mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext)
			.apply&lt;DefaultMockMvcBuilder&gt;(springSecurity())
			.build()
	}

	@Test
	fun loginWhenValidCsrfTokenThenSuccess() {
		mockMvc.perform(post("/login").with(csrf())
				.accept(MediaType.TEXT_HTML)
				.param("username", "user")
				.param("password", "password"))
			.andExpect(status().is3xxRedirection)
			.andExpect(header().string(HttpHeaders.LOCATION, "/"))
	}

	@Test
	fun loginWhenInvalidCsrfTokenThenForbidden() {
		mockMvc.perform(post("/login").with(csrf().useInvalidToken())
				.accept(MediaType.TEXT_HTML)
				.param("username", "user")
				.param("password", "password"))
			.andExpect(status().isForbidden)
	}

	@Test
	fun loginWhenMissingCsrfTokenThenForbidden() {
		mockMvc.perform(post("/login")
				.accept(MediaType.TEXT_HTML)
				.param("username", "user")
				.param("password", "password"))
			.andExpect(status().isForbidden)
	}

	@Test
	@WithMockUser
	@Throws(Exception::class)
	fun logoutWhenValidCsrfTokenThenSuccess() {
		mockMvc.perform(post("/logout").with(csrf())
				.accept(MediaType.TEXT_HTML))
			.andExpect(status().is3xxRedirection)
			.andExpect(header().string(HttpHeaders.LOCATION, "/login?logout"))
	}
}</span></code></pre>
</div>
</div>
</div>
</div>
</div>
</div>
</div>
<div class="sect1">
<h2 id="disable-csrf"><a class="anchor" href="#disable-csrf"></a>Disable CSRF Protection</h2>
<div class="sectionbody">
<div class="paragraph">
<p>By default, CSRF protection is enabled, which affects <a href="#csrf-integration">integrating with the backend</a> and <a href="#csrf-testing">testing</a> your application.
Before disabling CSRF protection, consider whether it <a href="../../features/exploits/csrf.html#csrf-when" class="xref page">makes sense for your application</a>.</p>
</div>
<div class="paragraph">
<p>You can also consider whether only certain endpoints do not require CSRF protection and configure an ignoring rule, as in the following example:</p>
</div>
<div id="disable-csrf-ignoring-configuration" class="openblock tabs is-sync is-loading">
<div class="title">Ignoring Requests</div>
<div class="content">
<div class="ulist tablist">
<ul>
<li id="disable_csrf_ignoring_configuration_java" class="tab">
<p>Java</p>
</li>
<li id="disable_csrf_ignoring_configuration_kotlin" class="tab">
<p>Kotlin</p>
</li>
<li id="disable_csrf_ignoring_configuration_xml" class="tab">
<p>XML</p>
</li>
</ul>
</div>
<div id="disable_csrf_ignoring_configuration_java--panel" class="tabpanel" aria-labelledby="disable_csrf_ignoring_configuration_java">
<div class="listingblock primary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ...
            .csrf((csrf) -&gt; csrf
                .ignoringRequestMatchers("/api/*")
            );
        return http.build();
    }
}</code></pre>
</div>
</div>
</div>
<div id="disable_csrf_ignoring_configuration_kotlin--panel" class="tabpanel" aria-labelledby="disable_csrf_ignoring_configuration_kotlin">
<div class="listingblock secondary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-kotlin hljs" data-lang="kotlin"><span class="fold-block is-hidden-folded">import org.springframework.security.config.annotation.web.invoke

</span><span class="fold-block">@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    open fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            // ...
            csrf {
                ignoringRequestMatchers("/api/*")
            }
        }
        return http.build()
    }
}</span></code></pre>
</div>
</div>
</div>
<div id="disable_csrf_ignoring_configuration_xml--panel" class="tabpanel" aria-labelledby="disable_csrf_ignoring_configuration_xml">
<div class="listingblock secondary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-xml hljs" data-lang="xml">&lt;http&gt;
	&lt;!-- ... --&gt;
	&lt;csrf request-matcher-ref="csrfMatcher"/&gt;
&lt;/http&gt;
&lt;b:bean id="csrfMatcher"
    class="org.springframework.security.web.util.matcher.AndRequestMatcher"&gt;
    &lt;b:constructor-arg value="#{T(org.springframework.security.web.csrf.CsrfFilter).DEFAULT_CSRF_MATCHER}"/&gt;
    &lt;b:constructor-arg&gt;
        &lt;b:bean class="org.springframework.security.web.util.matcher.NegatedRequestMatcher"&gt;
            &lt;b:bean class="org.springframework.security.config.http.PathPatternRequestMatcherFactoryBean"&gt;
                &lt;b:constructor-arg value="/api/*"/&gt;
            &lt;/b:bean&gt;
        &lt;/b:bean&gt;
    &lt;/b:constructor-arg&gt;
&lt;/b:bean&gt;</code></pre>
</div>
</div>
</div>
</div>
</div>
<div class="paragraph">
<p>If you need to disable CSRF protection, you can do so using the following configuration:</p>
</div>
<div id="disable-csrf-configuration" class="openblock tabs is-sync is-loading">
<div class="title">Disable CSRF</div>
<div class="content">
<div class="ulist tablist">
<ul>
<li id="disable_csrf_configuration_java" class="tab">
<p>Java</p>
</li>
<li id="disable_csrf_configuration_kotlin" class="tab">
<p>Kotlin</p>
</li>
<li id="disable_csrf_configuration_xml" class="tab">
<p>XML</p>
</li>
</ul>
</div>
<div id="disable_csrf_configuration_java--panel" class="tabpanel" aria-labelledby="disable_csrf_configuration_java">
<div class="listingblock primary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			// ...
			.csrf((csrf) -&gt; csrf.disable());
		return http.build();
	}
}</code></pre>
</div>
</div>
</div>
<div id="disable_csrf_configuration_kotlin--panel" class="tabpanel" aria-labelledby="disable_csrf_configuration_kotlin">
<div class="listingblock secondary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-kotlin hljs" data-lang="kotlin"><span class="fold-block is-hidden-folded">import org.springframework.security.config.annotation.web.invoke

</span><span class="fold-block">@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    open fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            // ...
            csrf {
                disable()
            }
        }
        return http.build()
    }
}</span></code></pre>
</div>
</div>
</div>
<div id="disable_csrf_configuration_xml--panel" class="tabpanel" aria-labelledby="disable_csrf_configuration_xml">
<div class="listingblock secondary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-xml hljs" data-lang="xml">&lt;http&gt;
	&lt;!-- ... --&gt;
	&lt;csrf disabled="true"/&gt;
&lt;/http&gt;</code></pre>
</div>
</div>
</div>
</div>
</div>
</div>
</div>
<div class="sect1">
<h2 id="csrf-considerations"><a class="anchor" href="#csrf-considerations"></a>CSRF Considerations</h2>
<div class="sectionbody">
<div class="paragraph">
<p>There are a few special considerations when implementing protection against CSRF attacks.
This section discusses those considerations as they pertain to servlet environments.
See <a href="../../features/exploits/csrf.html#csrf-considerations" class="xref page">CSRF Considerations</a> for a more general discussion.</p>
</div>
<div class="sect2">
<h3 id="csrf-considerations-login"><a class="anchor" href="#csrf-considerations-login"></a>Logging In</h3>
<div class="paragraph">
<p>It is important to <a href="../../features/exploits/csrf.html#csrf-considerations-login" class="xref page">require CSRF for log in</a> requests to protect against forging log in attempts.
Spring Security&#8217;s servlet support does this out of the box.</p>
</div>
</div>
<div class="sect2">
<h3 id="csrf-considerations-logout"><a class="anchor" href="#csrf-considerations-logout"></a>Logging Out</h3>
<div class="paragraph">
<p>It is important to <a href="../../features/exploits/csrf.html#csrf-considerations-logout" class="xref page">require CSRF for log out</a> requests to protect against forging logout attempts.
If CSRF protection is enabled (the default), Spring Security&#8217;s <code>LogoutFilter</code> will only process HTTP POST requests.
This ensures that logging out requires a CSRF token and that a malicious user cannot forcibly log your users out.</p>
</div>
<div class="paragraph">
<p>The easiest approach is to use a form to log the user out.
If you really want a link, you can use JavaScript to have the link perform a POST (maybe on a hidden form).
For browsers with JavaScript that is disabled, you can optionally have the link take the user to a log out confirmation page that performs the POST.</p>
</div>
<div class="paragraph">
<p>If you really want to use HTTP GET with logout, you can do so.
However, remember that this is generally not recommended.
For example, the following logs out when the <code>/logout</code> URL is requested with any HTTP method:</p>
</div>
<div id="_tabs_16" class="openblock tabs is-sync is-loading">
<div class="title">Log Out with Any HTTP Method</div>
<div class="content">
<div class="ulist tablist">
<ul>
<li id="_tabs_16_java" class="tab">
<p>Java</p>
</li>
<li id="_tabs_16_kotlin" class="tab">
<p>Kotlin</p>
</li>
</ul>
</div>
<div id="_tabs_16_java--panel" class="tabpanel" aria-labelledby="_tabs_16_java">
<div class="listingblock primary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			// ...
			.logout((logout) -&gt; logout
				.logoutRequestMatcher(PathPatternRequestMatcher.withDefaults().matcher("/logout"))
			);
		return http.build();
	}
}</code></pre>
</div>
</div>
</div>
<div id="_tabs_16_kotlin--panel" class="tabpanel" aria-labelledby="_tabs_16_kotlin">
<div class="listingblock secondary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-kotlin hljs" data-lang="kotlin"><span class="fold-block is-hidden-folded">import org.springframework.security.config.annotation.web.invoke

</span><span class="fold-block">@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    open fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            // ...
            logout {
                logoutRequestMatcher = PathPatternRequestMatcher.withDefaults().matcher("/logout")
            }
        }
        return http.build()
    }
}</span></code></pre>
</div>
</div>
</div>
</div>
</div>
<div class="paragraph">
<p>See the <a href="../authentication/logout.html" class="xref page">Logout</a> chapter for more information.</p>
</div>
</div>
<div class="sect2">
<h3 id="considerations-csrf-timeouts"><a class="anchor" href="#considerations-csrf-timeouts"></a>CSRF and Session Timeouts</h3>
<div class="paragraph">
<p>By default, Spring Security stores the CSRF token in the <code>HttpSession</code> using the <a href="#csrf-token-repository-httpsession"><code>HttpSessionCsrfTokenRepository</code></a>.
This can lead to a situation where the session expires, leaving no CSRF token to validate against.</p>
</div>
<div class="paragraph">
<p>We have already discussed <a href="../../features/exploits/csrf.html#csrf-considerations-timeouts" class="xref page">general solutions</a> to session timeouts.
This section discusses the specifics of CSRF timeouts as it pertains to the servlet support.</p>
</div>
<div class="paragraph">
<p>You can change the storage of the CSRF token to be in a cookie.
For details, see the <a href="#csrf-token-repository-cookie">Using the <code>CookieCsrfTokenRepository</code></a> section.</p>
</div>
<div class="paragraph">
<p>If a token does expire, you might want to customize how it is handled by specifying a <a href="#csrf-access-denied-handler">custom <code>AccessDeniedHandler</code></a>.
The custom <code>AccessDeniedHandler</code> can process the <code>InvalidCsrfTokenException</code> any way you like.</p>
</div>
</div>
<div class="sect2">
<h3 id="csrf-considerations-multipart"><a class="anchor" href="#csrf-considerations-multipart"></a>Multipart (file upload)</h3>
<div class="paragraph">
<p>We have <a href="../../features/exploits/csrf.html#csrf-considerations-multipart" class="xref page">already discussed</a> how protecting multipart requests (file uploads) from CSRF attacks causes a <a href="https://en.wikipedia.org/wiki/Chicken_or_the_egg">chicken and the egg</a> problem.
When JavaScript is available, we <em>recommend</em> <a href="#csrf-integration-javascript-other">including the CSRF token in an HTTP request header</a> to side-step the issue.</p>
</div>
<div class="paragraph">
<p>If JavaScript is not available, the following sections discuss options for placing the CSRF token in the <a href="#csrf-considerations-multipart-body">body</a> and <a href="#csrf-considerations-multipart-url">url</a> within a servlet application.</p>
</div>
<div class="admonitionblock note">
<table>
<tr>
<td class="icon">
<i class="fa icon-note" title="Note"></i>
</td>
<td class="content">
<div class="paragraph">
<p>You can find more information about using multipart forms with Spring in the <a href="https://docs.spring.io/spring-framework/reference/7.0.9/web/webmvc/mvc-servlet/multipart.html">Multipart Resolver</a> section of the Spring reference and the <a href="https://docs.spring.io/spring-framework/docs/7.0.9/javadoc-api/org/springframework/web/multipart/support/MultipartFilter.html"><code>MultipartFilter</code> javadoc</a>.</p>
</div>
</td>
</tr>
</table>
</div>
<div class="sect3">
<h4 id="csrf-considerations-multipart-body"><a class="anchor" href="#csrf-considerations-multipart-body"></a>Place CSRF Token in the Body</h4>
<div class="paragraph">
<p>We have <a href="../../features/exploits/csrf.html#csrf-considerations-multipart-body" class="xref page">already discussed</a> the tradeoffs of placing the CSRF token in the body.
In this section, we discuss how to configure Spring Security to read the CSRF from the body.</p>
</div>
<div class="paragraph">
<p>To read the CSRF token from the body, the <code>MultipartFilter</code> is specified before the Spring Security filter.
Specifying the <code>MultipartFilter</code> before the Spring Security filter means that there is no authorization for invoking the <code>MultipartFilter</code>, which means anyone can place temporary files on your server.
However, only authorized users can submit a file that is processed by your application.
In general, this is the recommended approach because the temporary file upload should have a negligible impact on most servers.</p>
</div>
<div id="_tabs_17" class="openblock tabs is-sync is-loading">
<div class="title">Configure <code>MultipartFilter</code></div>
<div class="content">
<div class="ulist tablist">
<ul>
<li id="_tabs_17_java" class="tab">
<p>Java</p>
</li>
<li id="_tabs_17_kotlin" class="tab">
<p>Kotlin</p>
</li>
<li id="_tabs_17_xml" class="tab">
<p>XML</p>
</li>
</ul>
</div>
<div id="_tabs_17_java--panel" class="tabpanel" aria-labelledby="_tabs_17_java">
<div class="listingblock primary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">public class SecurityApplicationInitializer extends AbstractSecurityWebApplicationInitializer {

	@Override
	protected void beforeSpringSecurityFilterChain(ServletContext servletContext) {
		insertFilters(servletContext, new MultipartFilter());
	}
}</code></pre>
</div>
</div>
</div>
<div id="_tabs_17_kotlin--panel" class="tabpanel" aria-labelledby="_tabs_17_kotlin">
<div class="listingblock secondary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-kotlin hljs" data-lang="kotlin">class SecurityApplicationInitializer : AbstractSecurityWebApplicationInitializer() {
    override fun beforeSpringSecurityFilterChain(servletContext: ServletContext?) {
        insertFilters(servletContext, MultipartFilter())
    }
}</code></pre>
</div>
</div>
</div>
<div id="_tabs_17_xml--panel" class="tabpanel" aria-labelledby="_tabs_17_xml">
<div class="listingblock secondary">
<div class="content">
<pre class="highlightjs highlight"><code class="language-xml hljs" data-lang="xml">&lt;filter&gt;
	&lt;filter-name&gt;MultipartFilter&lt;/filter-name&gt;
	&lt;filter-class&gt;org.springframework.web.multipart.support.MultipartFilter&lt;/filter-class&gt;
&lt;/filter&gt;
&lt;filter&gt;
	&lt;filter-name&gt;springSecurityFilterChain&lt;/filter-name&gt;
	&lt;filter-class&gt;org.springframework.web.filter.DelegatingFilterProxy&lt;/filter-class&gt;
&lt;/filter&gt;
&lt;filter-mapping&gt;
	&lt;filter-name&gt;MultipartFilter&lt;/filter-name&gt;
	&lt;url-pattern&gt;/*&lt;/url-pattern&gt;
&lt;/filter-mapping&gt;
&lt;filter-mapping&gt;
	&lt;filter-name&gt;springSecurityFilterChain&lt;/filter-name&gt;
	&lt;url-pattern&gt;/*&lt;/url-pattern&gt;
&lt;/filter-mapping&gt;</code></pre>
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
<p>To ensure that <code>MultipartFilter</code> is specified before the Spring Security filter with XML configuration, you can ensure the <code>&lt;filter-mapping&gt;</code> element of the <code>MultipartFilter</code> is placed before the <code>springSecurityFilterChain</code> within the <code>web.xml</code> file.</p>
</div>
</td>
</tr>
</table>
</div>
</div>
<div class="sect3">
<h4 id="csrf-considerations-multipart-url"><a class="anchor" href="#csrf-considerations-multipart-url"></a>Include a CSRF Token in a URL</h4>
<div class="paragraph">
<p>If letting unauthorized users upload temporary files is not acceptable, an alternative is to place the <code>MultipartFilter</code> after the Spring Security filter and include the CSRF as a query parameter in the action attribute of the form.
Since the <code>CsrfToken</code> is exposed as an <a href="#csrf-token-request-handler"><code>HttpServletRequest</code> attribute named <code>_csrf</code></a>, we can use that to create an <code>action</code> with the CSRF token in it.
The following example does this with a JSP:</p>
</div>
<div class="listingblock">
<div class="title">CSRF Token in Action</div>
<div class="content">
<pre class="highlightjs highlight"><code class="language-html hljs" data-lang="html">&lt;form method="post"
	action="./upload?${_csrf.parameterName}=${_csrf.token}"
	enctype="multipart/form-data"&gt;</code></pre>
</div>
</div>
</div>
</div>
<div class="sect2">
<h3 id="csrf-considerations-override-method"><a class="anchor" href="#csrf-considerations-override-method"></a>HiddenHttpMethodFilter</h3>
<div class="paragraph">
<p>We have <a href="../../features/exploits/csrf.html#csrf-considerations-multipart-body" class="xref page">already discussed</a> the trade-offs of placing the CSRF token in the body.</p>
</div>
<div class="paragraph">
<p>In Spring&#8217;s Servlet support, overriding the HTTP method is done by using <a href="https://docs.spring.io/spring-framework/docs/7.0.9/javadoc-api/org/springframework/web/filter/reactive/HiddenHttpMethodFilter.html"><code>HiddenHttpMethodFilter</code></a>.
You can find more information in the <a href="https://docs.spring.io/spring-framework/reference/7.0.9/web/webmvc-view/mvc-jsp.html#mvc-rest-method-conversion">HTTP Method Conversion</a> section of the reference documentation.</p>
</div>
</div>
</div>
</div>
<div class="sect1">
<h2 id="csrf-further-reading"><a class="anchor" href="#csrf-further-reading"></a>Further Reading</h2>
<div class="sectionbody">
<div class="paragraph">
<p>Now that you have reviewed CSRF protection, consider learning more about <a href="index.html" class="xref page">exploit protection</a> including <a href="headers.html" class="xref page">secure headers</a> and the <a href="firewall.html" class="xref page">HTTP firewall</a> or move on to learning how to <a href="../test/index.html" class="xref page">test</a> your application.</p>
</div>
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
    <a href="csrf.html">
      7.1.1
    </a>
  </li>
  <li class="version">
    <a href="../../7.0/servlet/exploits/csrf.html">
      7.0.7
    </a>
  </li>
  <li class="version">
    <a href="../../6.5/servlet/exploits/csrf.html">
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
    <a href="../../7.2/servlet/exploits/csrf.html">
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
    <a href="../../7.2-SNAPSHOT/servlet/exploits/csrf.html">
      7.2.0-SNAPSHOT
    </a>
  </li>
  <li class="version">
    <a href="../../7.1-SNAPSHOT/servlet/exploits/csrf.html">
      7.1.2-SNAPSHOT
    </a>
  </li>
  <li class="version">
    <a href="../../7.0-SNAPSHOT/servlet/exploits/csrf.html">
      7.0.8-SNAPSHOT
    </a>
  </li>
  <li class="version">
    <a href="../../6.5-SNAPSHOT/servlet/exploits/csrf.html">
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

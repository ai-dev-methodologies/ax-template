<!DOCTYPE html>
<html lang="en">
  <script src="https://cdn.cookielaw.org/scripttemplates/otSDKStub.js" data-domain-script="018ee325-b3a7-7753-937b-b8b3e643b1a7"></script><script>function OptanonWrapper() {}</script><script>function setGTM(w, d, s, l, i) { w[l] = w[l] || []; w[l].push({ "gtm.start": new Date().getTime(), event: "gtm.js"}); var f = d.getElementsByTagName(s)[0], j = d.createElement(s), dl = l != "dataLayer" ? "&l=" + l : ""; j.async = true; j.src = "https://www.googletagmanager.com/gtm.js?id=" + i + dl; f.parentNode.insertBefore(j, f); } if (document.cookie.indexOf("OptanonConsent") > -1 && document.cookie.indexOf("groups=") > -1) { setGTM(window, document, "script", "dataLayer", "GTM-W8CQ8TL"); } else { waitForOnetrustActiveGroups(); } var timer; function waitForOnetrustActiveGroups() { if (document.cookie.indexOf("OptanonConsent") > -1 && document.cookie.indexOf("groups=") > -1) { clearTimeout(timer); setGTM(window, document, "script", "dataLayer", "GTM-W8CQ8TL"); } else { timer = setTimeout(waitForOnetrustActiveGroups, 250); }}</script>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>JPA Query Methods :: Spring Data JPA</title>
    <link rel="canonical" href="https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html">
    <meta name="generator" content="Antora 3.2.0-alpha.12">
    <script>
!function (theme, navWidth) {
  if (theme === 'dark') document.documentElement.classList.add('dark-theme')
  if (navWidth) document.documentElement.style.setProperty('--nav-width', `${navWidth}px`)
}(localStorage && localStorage.getItem('theme') || (matchMedia('(prefers-color-scheme: dark)')?.matches && 'dark'),
  localStorage && localStorage.getItem('nav-width'))
    </script>
    <link rel="stylesheet" href="../_/css/site.css">
    <link rel="stylesheet" href="../_/css/vendor/search.css">
    <link rel="stylesheet" href="../_/css/vendor/page-search.css">
    <link rel="stylesheet" href="../_/css/vendor/onetrust.css">
    <link rel="stylesheet" href="../_/css/vendor/asciidoctor-tabs.css">

    <meta name="antora-ui-version" content="v0.4.25"> 
    <meta name="version" content="4.1.1">
    <meta name="generation" content="4.1">
    <meta name="versioned-url" content="https://docs.spring.io/spring-data/jpa/reference/4.1/jpa/query-methods.html">
    <meta name="component" content="data-jpa">
    <meta name="latest-version" content="true">
    <link rel="icon" href="../_/img/favicon.ico" type="image/vnd.microsoft.icon">
  </head>
  <body class="article">
<header class="header">
  <nav class="navbar">
    <div class="navbar-brand">
      <a class="navbar-item" href="https://spring.io">
        <img
          id="springlogo"
          class="block"
          src="../_/img/spring-logo.svg"
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
            <a
              class="navbar-item"
              href="https://spring.io/why-spring"
            >Overview</a>
            <a
              class="navbar-item"
              href="https://spring.io/microservices"
            >Microservices</a>
            <a
              class="navbar-item"
              href="https://spring.io/reactive"
            >Reactive</a>
            <a class="navbar-item" href="https://spring.io/event-driven">Event
              Driven</a>
            <a class="navbar-item" href="https://spring.io/cloud">Cloud</a>
            <a class="navbar-item" href="https://spring.io/web-applications">Web
              Applications</a>
            <a
              class="navbar-item"
              href="https://spring.io/serverless"
            >Serverless</a>
            <a class="navbar-item" href="https://spring.io/batch">Batch</a>
          </div>
        </div>
        <div class="navbar-item has-dropdown is-hoverable">
          <a class="navbar-link" href="#">Learn</a>
          <div class="navbar-dropdown">
            <a class="navbar-item" href="https://spring.io/learn">Overview</a>
            <a
              class="navbar-item"
              href="https://spring.io/quickstart"
            >Quickstart</a>
            <a class="navbar-item" href="https://spring.io/guides">Guides</a>
            <a class="navbar-item" href="https://spring.io/blog">Blog</a>
          </div>
        </div>
        <div class="navbar-item has-dropdown is-hoverable">
          <a class="navbar-link" href="#">Projects</a>
          <div class="navbar-dropdown" style="min-width: 280px">
            <a
              class="navbar-item"
              href="https://spring.io/projects"
            >Overview</a>
            <a
              class="navbar-item"
              href="https://spring.io/projects/spring-boot"
            >Spring Boot</a>
            <a
              class="navbar-item"
              href="https://spring.io/projects/spring-framework"
            >Spring Framework</a>
            <a
              class="navbar-item"
              href="https://spring.io/projects/spring-cloud"
            >Spring Cloud</a>
            <a
              class="navbar-item"
              href="https://spring.io/projects/spring-cloud-dataflow"
            >Spring Cloud Data Flow</a>
            <a
              class="navbar-item"
              href="https://spring.io/projects/spring-data"
            >Spring Data</a>
            <a
              class="navbar-item"
              href="https://spring.io/projects/spring-integration"
            >Spring Integration</a>
            <a
              class="navbar-item"
              href="https://spring.io/projects/spring-batch"
            >Spring Batch</a>
            <a
              class="navbar-item"
              href="https://spring.io/projects/spring-security"
            >Spring Security</a>
            <a
              class="navbar-item navbar-item-special"
              href="https://spring.io/projects"
            >View all projects</a>
            <li class="navbar-item navbar-item-special-3">DEVELOPMENT TOOLS</li>
            <a class="navbar-item" href="https://spring.io/tools">Spring Tools 4</a>
            <a
              class="navbar-item navbar-item-special-2"
              href="https://start.spring.io"
            >Spring Initializr
              <svg
                class="external-link-icon"
                xmlns="http://www.w3.org/2000/svg"
                viewBox="0 0 16 16"
              ><polyline
                  points="15 10.94 15 15 1 15 1 1 5.06 1"
                  fill="none"
                  stroke="currentColor"
                  stroke-miterlimit="10"
                  stroke-width="2"
                ></polyline><polyline
                  points="8.93 1 15 1 15 7.07"
                  fill="none"
                  stroke="currentColor"
                  stroke-miterlimit="10"
                  stroke-width="2"
                ></polyline><line
                  x1="15"
                  y1="1"
                  x2="8"
                  y2="8"
                  fill="none"
                  stroke="currentColor"
                  stroke-miterlimit="10"
                  stroke-width="2"
                ></line></svg></a>
          </div>
        </div>

        <div class="navbar-item has-dropdown is-hoverable">
          <a class="navbar-link" href="#">Academy</a>
          <div class="navbar-dropdown">
            <a
              class="navbar-item"
              href="https://spring.academy/courses"
            >Courses</a>
            <a
              class="navbar-item"
              href="https://spring.academy/learning-path"
            >Get Certified</a>
          </div>
        </div>

        <div class="navbar-item has-dropdown is-hoverable">
          <a class="navbar-link" href="#">Solutions</a>
          <div class="navbar-dropdown lg">
            <a class="navbar-item" href="https://spring.io/solutions">Overview</a>
            <a class="navbar-item" href="https://spring.io/support">Spring Runtime</a>
            <a class="navbar-item" href="https://spring.io/consulting">Spring Consulting</a>
            <a class="navbar-item" href="https://spring.academy/teams">Spring Academy For Teams</a>
            <a class="navbar-item" href="https://spring.io/security">Security Advisories</a>
          </div>
        </div>

        <div class="navbar-item has-dropdown is-hoverable is-community">
          <a class="navbar-link" href="#">Community</a>
          <div class="navbar-dropdown">
            <a
              class="navbar-item"
              href="https://spring.io/community"
            >Overview</a>
            <a class="navbar-item" href="https://spring.io/events">Events</a>
            <a class="navbar-item" href="https://spring.io/team">Team</a>
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
<div class="nav-container" data-component="data-jpa" data-version="4.1.1">
  <aside class="nav">
    <div class="panels">
      <div class="nav-panel-menu is-active" data-panel="menu">
        <nav class="nav-menu">
<div class="context">
  <span class="title">Spring Data JPA</span>
  <span class="version">4.1.1</span>
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
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../index.html">Overview</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../commons/upgrade.html">Upgrading Spring Data</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="1">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../jpa.html">JPA</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="getting-started.html">Getting Started</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../repositories/core-concepts.html">Core concepts</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../repositories/definition.html">Defining Repository Interfaces</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../repositories/create-instances.html">Configuration</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="entity-persistence.html">Persisting Entities</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../repositories/query-methods-details.html">Defining Query Methods</a>
  </li>
  <li class="nav-item is-current-page" data-depth="2">
    <a class="nav-link"  href="query-methods.html">JPA Query Methods</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="value-expressions.html">Value Expressions Fundamentals</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../repositories/projections.html">Projections</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="stored-procedures.html">Stored Procedures</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="specifications.html">Specifications</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../repositories/query-by-example.html">Query by Example</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../repositories/vector-search.html">Vector Search</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="transactions.html">Transactionality</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="locking.html">Locking</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../auditing.html">Auditing</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="misc-merging-persistence-units.html">Merging persistence units</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="jpd-misc-cdi-integration.html">CDI Integration</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../repositories/custom-implementations.html">Custom Repository Implementations</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../repositories/core-domain-events.html">Publishing Events from Aggregate Roots</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../repositories/null-handling.html">Null Handling of Repository Methods</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../repositories/core-extensions.html">Spring Data Extensions</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../repositories/query-keywords-reference.html">Repository query keywords</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../repositories/query-return-types-reference.html">Repository query return types</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="aot.html">Ahead of Time Optimizations</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="faq.html">Frequently Asked Questions</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="glossary.html">Glossary</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="1">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../envers.html">Envers</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../envers/introduction.html">Introduction</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../envers/configuration.html">Configuration</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../envers/usage.html">Usage</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="1">
    <a class="nav-link attachment link-external"  target="_blank" href="../api/java/index.html">Javadoc</a>
  </li>
  <li class="nav-item" data-depth="1">
    <a class="nav-link link-external"  target="_blank" href="https://github.com/spring-projects/spring-data-commons/wiki">Wiki</a>
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
      data-title="JPA Query Methods"
      data-levels="2"
    >
      <div class="toc-menu"></div>
    </div>
    <div class="sidebar-links">
        <a href="https://github.com/spring-projects/spring-data-jpa/blob/4.1.1/src/main/antora/modules/ROOT/pages/jpa/query-methods.adoc">
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
              <a href="https://github.com/spring-projects/spring-data-jpa" title="GitHub">
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
        <a href="https://stackoverflow.com/tags/spring-data-jpa">
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
      <li><a href="../index.html">Spring Data JPA</a></li>
      <li><a href="../jpa.html">JPA</a></li>
      <li><a href="query-methods.html">JPA Query Methods</a></li>
    </ul>
  </nav>
</div><h1 id="page-title" class="page">JPA Query Methods</h1>
<div id="preamble">
<div class="sectionbody">
<div class="paragraph">
<p>This section describes the various ways to create a query with Spring Data JPA.</p>
</div>
</div>
</div>
<div class="sect1">
<h2 id="jpa.sample-app.finders.strategies"><a class="anchor" href="#jpa.sample-app.finders.strategies"></a>Query Lookup Strategies</h2>
<div class="sectionbody">
<div class="paragraph">
<p>The JPA module supports defining a query manually as a String or having it being derived from the method name.</p>
</div>
<div class="paragraph">
<p>Derived queries with the predicates <code>IsStartingWith</code>, <code>StartingWith</code>, <code>StartsWith</code>, <code>IsEndingWith</code>, <code>EndingWith</code>, <code>EndsWith</code>,
<code>IsNotContaining</code>, <code>NotContaining</code>, <code>NotContains</code>, <code>IsContaining</code>, <code>Containing</code>, <code>Contains</code> the respective arguments for these queries will get sanitized.
This means if the arguments actually contain characters recognized by <code>LIKE</code> as wildcards these will get escaped so they match only as literals.
The escape character used can be configured by setting the <code>escapeCharacter</code> of the <code>@EnableJpaRepositories</code> annotation.
Compare with <a href="#jpa.query.spel-expressions">Using Value Expressions</a>.</p>
</div>
<div class="sect2">
<h3 id="jpa.query-methods.declared-queries"><a class="anchor" href="#jpa.query-methods.declared-queries"></a>Declared Queries</h3>
<div class="paragraph">
<p>Although getting a query derived from the method name is quite convenient, one might face the situation in which either the method name parser does not support the keyword one wants to use or the method name would get unnecessarily ugly. So you can either use JPA named queries through a naming convention (see <a href="#jpa.query-methods.named-queries">Using JPA Named Queries</a> for more information) or rather annotate your query method with <code>@Query</code> (see <a href="#jpa.query-methods.at-query">Using <code>@Query</code></a> for details).</p>
</div>
</div>
</div>
</div>
<div class="sect1">
<h2 id="jpa.query-methods.query-creation"><a class="anchor" href="#jpa.query-methods.query-creation"></a>Query Creation</h2>
<div class="sectionbody">
<div class="paragraph">
<p>Generally, the query creation mechanism for JPA works as described in <a href="https://docs.spring.io/spring-data/commons/reference/4.1/repositories/query-methods.html">Query Methods</a>. The following example shows what a JPA query method translates into:</p>
</div>
<div class="exampleblock">
<div class="title">Example 1. Query creation from method names</div>
<div class="content">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">public interface UserRepository extends Repository&lt;User, Long&gt; {

  List&lt;User&gt; findByEmailAddressAndLastname(String emailAddress, String lastname);
}</code></pre>
</div>
</div>
<div class="paragraph">
<p>We create a query using JPQL  translating into the following query: <code>select u from User u where u.emailAddress = ?1 and u.lastname = ?2</code>. Spring Data JPA does a property check and traverses nested properties, as described in <a href="../repositories/query-methods-details.html#repositories.query-methods.query-property-expressions" class="xref page">Property Expressions</a>.</p>
</div>
</div>
</div>
<div class="paragraph">
<p>The following table describes the keywords supported for JPA and what a method containing that keyword translates to:</p>
</div>
<table class="tableblock frame-all grid-all fit-content">
<caption class="title">Table 1. Supported keywords inside method names</caption>
<colgroup>
<col>
<col>
<col>
</colgroup>
<thead>
<tr>
<th class="tableblock halign-left valign-top">Keyword</th>
<th class="tableblock halign-left valign-top">Sample</th>
<th class="tableblock halign-left valign-top">JPQL snippet</th>
</tr>
</thead>
<tbody>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>Distinct</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>findDistinctByLastnameAndFirstname</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>select distinct &#8230;&#8203; where x.lastname = ?1 and x.firstname = ?2</code></p></td>
</tr>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>And</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>findByLastnameAndFirstname</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>… where x.lastname = ?1 and x.firstname = ?2</code></p></td>
</tr>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>Or</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>findByLastnameOrFirstname</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>… where x.lastname = ?1 or x.firstname = ?2</code></p></td>
</tr>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>Is</code>, <code>Equals</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>findByFirstname</code>,<code>findByFirstnameIs</code>,<code>findByFirstnameEquals</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>… where x.firstname = ?1</code> (or <code>… where x.firstname IS NULL</code> if the argument is <code>null</code>)</p></td>
</tr>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>Between</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>findByStartDateBetween</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>… where x.startDate between ?1 and ?2</code></p></td>
</tr>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>LessThan</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>findByAgeLessThan</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>… where x.age &lt; ?1</code></p></td>
</tr>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>LessThanEqual</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>findByAgeLessThanEqual</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>… where x.age &lt;= ?1</code></p></td>
</tr>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>GreaterThan</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>findByAgeGreaterThan</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>… where x.age &gt; ?1</code></p></td>
</tr>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>GreaterThanEqual</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>findByAgeGreaterThanEqual</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>… where x.age &gt;= ?1</code></p></td>
</tr>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>After</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>findByStartDateAfter</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>… where x.startDate &gt; ?1</code></p></td>
</tr>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>Before</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>findByStartDateBefore</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>… where x.startDate &lt; ?1</code></p></td>
</tr>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>IsNull</code>, <code>Null</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>findByAge(Is)Null</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>… where x.age is null</code></p></td>
</tr>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>IsNotNull</code>, <code>NotNull</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>findByAge(Is)NotNull</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>… where x.age is not null</code></p></td>
</tr>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>Like</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>findByFirstnameLike</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>… where x.firstname like ?1</code></p></td>
</tr>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>NotLike</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>findByFirstnameNotLike</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>… where x.firstname not like ?1</code></p></td>
</tr>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>StartingWith</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>findByFirstnameStartingWith</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>… where x.firstname like ?1</code> (parameter bound with appended <code>%</code>)</p></td>
</tr>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>EndingWith</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>findByFirstnameEndingWith</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>… where x.firstname like ?1</code> (parameter bound with prepended <code>%</code>)</p></td>
</tr>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>Containing</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>findByFirstnameContaining</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>… where x.firstname like ?1</code> (parameter bound wrapped in <code>%</code>)</p></td>
</tr>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>OrderBy</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>findByAgeOrderByLastnameDesc</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>… where x.age = ?1 order by x.lastname desc</code></p></td>
</tr>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>Not</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>findByLastnameNot</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>… where x.lastname &lt;&gt; ?1</code> (or <code>… where x.lastname IS NOT NULL</code> if the argument is <code>null</code>)</p></td>
</tr>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>In</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>findByAgeIn(Collection&lt;Age&gt; ages)</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>… where x.age in ?1</code></p></td>
</tr>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>NotIn</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>findByAgeNotIn(Collection&lt;Age&gt; ages)</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>… where x.age not in ?1</code></p></td>
</tr>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>True</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>findByActiveTrue()</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>… where x.active = true</code></p></td>
</tr>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>False</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>findByActiveFalse()</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>… where x.active = false</code></p></td>
</tr>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>IgnoreCase</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>findByFirstnameIgnoreCase</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>… where UPPER(x.firstname) = UPPER(?1)</code></p></td>
</tr>
</tbody>
</table>
<div class="admonitionblock note">
<table>
<tr>
<td class="icon">
<i class="fa icon-note" title="Note"></i>
</td>
<td class="content">
<code>In</code> and <code>NotIn</code> also take any subclass of <code>Collection</code> as a parameter as well as arrays or varargs. For other syntactical versions of the same logical operator, check <a href="../repositories/query-keywords-reference.html" class="xref page">Repository query keywords</a>.
</td>
</tr>
</table>
</div>
<div class="admonitionblock warning">
<table>
<tr>
<td class="icon">
<i class="fa icon-warning" title="Warning"></i>
</td>
<td class="content">
<div class="paragraph">
<p><code>DISTINCT</code> can be tricky and not always producing the results you expect.
For example, <code>select distinct u from User u</code> will produce a complete different result than <code>select distinct u.lastname from User u</code>.
In the first case, since you are including <code>User.id</code>, nothing will be duplicated, hence you&#8217;ll get the whole table, and it would be of <code>User</code> objects.</p>
</div>
<div class="paragraph">
<p>However, that latter query would narrow the focus to just <code>User.lastname</code> and find all unique last names for that table.
This would also yield a <code>List&lt;String&gt;</code> result set instead of a <code>List&lt;User&gt;</code> result set.</p>
</div>
<div class="paragraph">
<p><code>countDistinctByLastname(String lastname)</code> can also produce unexpected results.
Spring Data JPA will derive <code>select count(distinct u.id) from User u where u.lastname = ?1</code>.
Again, since <code>u.id</code> won&#8217;t hit any duplicates, this query will count up all the users that had the binding last name.
Which would be the same as <code>countByLastname(String lastname)</code>!</p>
</div>
<div class="paragraph">
<p>What is the point of this query anyway? To find the number of people with a given last name? To find the number of <em>distinct</em> people with that binding last name?
To find the number of <em>distinct last names</em>? (That last one is an entirely different query!)
Using <code>distinct</code> sometimes requires writing the query by hand and using <code>@Query</code> to best capture the information you seek, since you also may be needing a projection
to capture the result set.</p>
</div>
</td>
</tr>
</table>
</div>
<div class="sect2">
<h3 id="jpa.query-methods.named-queries.annotation-based-configuration"><a class="anchor" href="#jpa.query-methods.named-queries.annotation-based-configuration"></a>Annotation-based Configuration</h3>
<div class="paragraph">
<p>Annotation-based configuration has the advantage of not needing another configuration file to be edited, lowering maintenance effort. You pay for that benefit by the need to recompile your domain class for every new query declaration.</p>
</div>
<div class="exampleblock">
<div class="title">Example 2. Annotation-based named query configuration</div>
<div class="content">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Entity
@NamedQuery(name = "User.findByEmailAddress",
  query = "select u from User u where u.emailAddress = ?1")
public class User {

}</code></pre>
</div>
</div>
</div>
</div>
</div>
</div>
</div>
<div class="sect1">
<h2 id="jpa.query-methods.named-queries"><a class="anchor" href="#jpa.query-methods.named-queries"></a>Using JPA Named Queries</h2>
<div class="sectionbody">
<div class="admonitionblock note">
<table>
<tr>
<td class="icon">
<i class="fa icon-note" title="Note"></i>
</td>
<td class="content">
The examples use the <code>&lt;named-query /&gt;</code> element and <code>@NamedQuery</code> annotation. The queries for these configuration elements have to be defined in the JPA query language. Of course, you can use <code>&lt;named-native-query /&gt;</code> or <code>@NamedNativeQuery</code> too. These elements let you define the query in native SQL by losing the database platform independence.
</td>
</tr>
</table>
</div>
<div class="sect2">
<h3 id="jpa.query-methods.named-queries.xml-named-query-definition"><a class="anchor" href="#jpa.query-methods.named-queries.xml-named-query-definition"></a>XML Named Query Definition</h3>
<div class="paragraph">
<p>To use XML configuration, add the necessary <code>&lt;named-query /&gt;</code> element to the <code>orm.xml</code> JPA configuration file located in the <code>META-INF</code> folder of your classpath. Automatic invocation of named queries is enabled by using some defined naming convention. For more details, see below.</p>
</div>
<div class="exampleblock">
<div class="title">Example 3. XML named query configuration</div>
<div class="content">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-xml hljs" data-lang="xml">&lt;named-query name="User.findByLastname"&gt;
  &lt;query&gt;select u from User u where u.lastname = ?1&lt;/query&gt;
&lt;/named-query&gt;</code></pre>
</div>
</div>
</div>
</div>
<div class="paragraph">
<p>The query has a special name that is used to resolve it at runtime.</p>
</div>
</div>
<div class="sect2">
<h3 id="jpa.query-methods.named-queries.declaring-interfaces"><a class="anchor" href="#jpa.query-methods.named-queries.declaring-interfaces"></a>Declaring Interfaces</h3>
<div class="paragraph">
<p>To allow these named queries, specify the <code>UserRepository</code> as follows:</p>
</div>
<div class="exampleblock">
<div class="title">Example 4. Query method declaration in UserRepository</div>
<div class="content">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">public interface UserRepository extends JpaRepository&lt;User, Long&gt; {

  List&lt;User&gt; findByLastname(String lastname);

  User findByEmailAddress(String emailAddress);
}</code></pre>
</div>
</div>
</div>
</div>
<div class="paragraph">
<p>Spring Data tries to resolve a call to these methods to a named query, starting with the simple name of the configured domain class, followed by the method name separated by a dot.
So the preceding example would use the named queries defined earlier instead of trying to create a query from the method name.</p>
</div>
</div>
</div>
</div>
<div class="sect1">
<h2 id="jpa.query-methods.at-query"><a class="anchor" href="#jpa.query-methods.at-query"></a>Using <code>@Query</code></h2>
<div class="sectionbody">
<div class="paragraph">
<p>Using named queries to declare queries for entities is a valid approach and works fine for a small number of queries. As the queries themselves are tied to the Java method that runs them, you can actually bind them directly by using the Spring Data JPA <code>@Query</code> annotation rather than annotating them to the domain class. This frees the domain class from persistence specific information and co-locates the query to the repository interface.</p>
</div>
<div class="paragraph">
<p>Queries annotated to the query method take precedence over queries defined using <code>@NamedQuery</code> or named queries declared in <code>orm.xml</code>.</p>
</div>
<div class="paragraph">
<p>The following example shows a query created with the <code>@Query</code> annotation:</p>
</div>
<div class="exampleblock">
<div class="title">Example 5. Declare query at the query method using <code>@Query</code></div>
<div class="content">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">public interface UserRepository extends JpaRepository&lt;User, Long&gt; {

  @Query("select u from User u where u.emailAddress = ?1")
  User findByEmailAddress(String emailAddress);
}</code></pre>
</div>
</div>
</div>
</div>
<div class="sect2">
<h3 id="jpa.query-methods.at-query.advanced-like"><a class="anchor" href="#jpa.query-methods.at-query.advanced-like"></a>Using Advanced <code>LIKE</code> Expressions</h3>
<div class="paragraph">
<p>The query running mechanism for manually defined queries created with <code>@Query</code> allows the definition of advanced <code>LIKE</code> expressions inside the query definition, as shown in the following example:</p>
</div>
<div class="exampleblock">
<div class="title">Example 6. Advanced <code>like</code> expressions in @Query</div>
<div class="content">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">public interface UserRepository extends JpaRepository&lt;User, Long&gt; {

  @Query("select u from User u where u.firstname like %?1")
  List&lt;User&gt; findByFirstnameEndsWith(String firstname);
}</code></pre>
</div>
</div>
</div>
</div>
<div class="paragraph">
<p>In the preceding example, the <code>LIKE</code> delimiter character (<code>%</code>) is recognized, and the query is transformed into a valid JPQL query (removing the <code>%</code>). Upon running the query, the parameter passed to the method call gets augmented with the previously recognized <code>LIKE</code> pattern.</p>
</div>
</div>
<div class="sect2">
<h3 id="jpa.query-methods.at-query.native"><a class="anchor" href="#jpa.query-methods.at-query.native"></a>Native Queries</h3>
<div class="paragraph">
<p>Using the <code>@NativeQuery</code> annotation allows running native queries, as shown in the following example:</p>
</div>
<div class="exampleblock">
<div class="title">Example 7. Declare a native query at the query method using <code>@NativeQuery</code></div>
<div class="content">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">public interface UserRepository extends JpaRepository&lt;User, Long&gt; {

  @NativeQuery(value = "SELECT * FROM USERS WHERE EMAIL_ADDRESS = ?1")
  User findByEmailAddress(String emailAddress);
}</code></pre>
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
The <code>@NativeQuery</code> annotation is mostly a composed annotation for <code>@Query(nativeQuery=true)</code> but it also provides additional attributes such as <code>sqlResultSetMapping</code> to leverage JPA&#8217;s <code>@SqlResultSetMapping(…)</code>.
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
Spring Data can rewrite simple queries for pagination and sorting.
More complex queries require either <a href="https://github.com/JSQLParser/JSqlParser">JSqlParser</a> to be on the class path or a <code>countQuery</code> declared in your code.
See the example below for more details.
</td>
</tr>
</table>
</div>
<div class="exampleblock">
<div class="title">Example 8. Declare native count queries for pagination at the query method by using <code>@NativeQuery</code></div>
<div class="content">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">public interface UserRepository extends JpaRepository&lt;User, Long&gt; {

  @NativeQuery(value = "SELECT * FROM USERS WHERE LASTNAME = ?1",
    countQuery = "SELECT count(*) FROM USERS WHERE LASTNAME = ?1")
  Page&lt;User&gt; findByLastname(String lastname, Pageable pageable);
}</code></pre>
</div>
</div>
</div>
</div>
<div class="paragraph">
<p>A similar approach also works with named native queries, by adding the <code>.count</code> suffix to a copy of your query. You probably need to register a result set mapping for your count query, though.</p>
</div>
<div class="paragraph">
<p>Next to obtaining mapped results, native queries allow you to read the raw <code>Tuple</code> from the database by choosing a <code>Map</code> container as the method&#8217;s return type.
The resulting map contains key/value pairs representing the actual database column name and the value.</p>
</div>
<div class="exampleblock">
<div class="title">Example 9. Native query returning raw column name/value pairs</div>
<div class="content">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">interface UserRepository extends JpaRepository&lt;User, Long&gt; {

  @NativeQuery("SELECT * FROM USERS WHERE EMAIL_ADDRESS = ?1")
  Map&lt;String, Object&gt; findRawMapByEmail(String emailAddress);      <i class="conum" data-value="1"></i><b>(1)</b>

  @NativeQuery("SELECT * FROM USERS WHERE LASTNAME = ?1")
  List&lt;Map&lt;String, Object&gt;&gt; findRawMapByLastname(String lastname); <i class="conum" data-value="2"></i><b>(2)</b>
}</code></pre>
</div>
</div>
<div class="colist arabic">
<table>
<tr>
<td><i class="conum" data-value="1"></i><b>1</b></td>
<td>Single <code>Map</code> result backed by a <code>Tuple</code>.</td>
</tr>
<tr>
<td><i class="conum" data-value="2"></i><b>2</b></td>
<td>Multiple <code>Map</code> results backed by <code>Tuple</code>s.</td>
</tr>
</table>
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
String-based Tuple Queries are only supported by Hibernate.
Eclipselink supports only Criteria-based Tuple Queries.
</td>
</tr>
</table>
</div>
</div>
<div class="sect2">
<h3 id="jpa.query-methods.query-introspection-rewriting"><a class="anchor" href="#jpa.query-methods.query-introspection-rewriting"></a>Query Introspection and Rewriting</h3>
<div class="paragraph">
<p>Spring Data JPA provides a wide range of functionality that can be used to run various flavors of queries.
Specifically, given a declared query, Spring Data JPA can:</p>
</div>
<div class="ulist">
<ul>
<li>
<p>Introspect a query for its projection and run a tuple query for interface projections</p>
</li>
<li>
<p>Use DTO projections if the query uses constructor expressions and rewrite the projection when the query declares the entity alias or just a multi-select of expressions</p>
</li>
<li>
<p>Apply dynamic sorting</p>
</li>
<li>
<p>Derive a <code>COUNT</code> query</p>
</li>
</ul>
</div>
<div class="paragraph">
<p>For this purpose, we ship with Query Parsers specific to HQL (Hibernate) and EQL (EclipseLink) dialects as these dialects are well-defined.
SQL on the other hand allows for quite some variance across dialects.
Because of this, there is no way Spring Data will ever be able to support all levels of query complexity.
We are not general purpose SQL parser library but one to increase developer productivity through making query execution simpler.
Our built-in SQL query enhancer supports only simple queries for introspection <code>COUNT</code> query derivation.
A more complex query will require either the usage of <a href="https://github.com/JSQLParser/JSqlParser">JSqlParser</a> or that you provide a <code>COUNT</code> query through <code>@Query(countQuery=…)</code>.
If JSqlParser is on the class path, Spring Data JPA will use it for native queries.</p>
</div>
<div class="paragraph">
<p>For a fine-grained control over selection, you can configure <a href="../api/java/org/springframework/data/jpa/repository/query/QueryEnhancerSelector.html" class="xref attachment apiref"><code>QueryEnhancerSelector</code></a> using <code>@EnableJpaRepositories</code>:</p>
</div>
<div class="exampleblock">
<div class="title">Example 10. Spring Data JPA repositories using JavaConfig</div>
<div class="content">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Configuration
@EnableJpaRepositories(queryEnhancerSelector = MyQueryEnhancerSelector.class)
class ApplicationConfig {
  // …
}</code></pre>
</div>
</div>
</div>
</div>
<div class="paragraph">
<p><code>QueryEnhancerSelector</code> is a strategy interface intended to select a <a href="../api/java/org/springframework/data/jpa/repository/query/QueryEnhancer.html" class="xref attachment apiref"><code>QueryEnhancer</code></a> based on a specific query.
You can also provide your own <code>QueryEnhancer</code> implementation if you want.</p>
</div>
</div>
<div class="sect2">
<h3 id="jpa.query-methods.query-rewriter"><a class="anchor" href="#jpa.query-methods.query-rewriter"></a>Applying a QueryRewriter</h3>
<div class="paragraph">
<p>Sometimes, no matter how many features you try to apply, it seems impossible to get Spring Data JPA to apply every thing you&#8217;d like to a query before it is sent to the <code>EntityManager</code>.</p>
</div>
<div class="paragraph">
<p>You have the ability to get your hands on the query, right before it&#8217;s sent to the <code>EntityManager</code> and "rewrite" it.
That is, you can make any alterations at the last moment.
Query rewriting applies to the actual query and, when applicable, to count queries.
Count queries are optimized and therefore, either not necessary or a count is obtained through other means, such as derived from a Hibernate <code>SelectionQuery</code> if there is an enclosing transaction.</p>
</div>
<div class="exampleblock">
<div class="title">Example 11. Declare a QueryRewriter using <code>@Query</code> and <code>@NativeQuery</code></div>
<div class="content">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">public interface MyRepository extends JpaRepository&lt;User, Long&gt; {

		@NativeQuery(value = "select original_user_alias.* from SD_USER original_user_alias",
				queryRewriter = MyQueryRewriter.class)
		List&lt;User&gt; findByNativeQuery(String param);

		@Query(value = "select original_user_alias from User original_user_alias",
                queryRewriter = MyQueryRewriter.class)
		List&lt;User&gt; findByNonNativeQuery(String param);
}</code></pre>
</div>
</div>
</div>
</div>
<div class="paragraph">
<p>This example shows both a native (pure SQL) rewriter as well as a JPQL query, both leveraging the same <code>QueryRewriter</code>.
In this scenario, Spring Data JPA will look for a bean registered in the application context of the corresponding type.</p>
</div>
<div class="paragraph">
<p>You can write a query rewriter like this:</p>
</div>
<div class="exampleblock">
<div class="title">Example 12. Example <code>QueryRewriter</code></div>
<div class="content">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">public class MyQueryRewriter implements QueryRewriter {

     @Override
     public String rewrite(String query, Sort sort) {
         return query.replaceAll("original_user_alias", "rewritten_user_alias");
     }
}</code></pre>
</div>
</div>
</div>
</div>
<div class="paragraph">
<p>You have to ensure your <code>QueryRewriter</code> is registered in the application context, whether it&#8217;s by applying one of Spring Framework&#8217;s
<code>@Component</code>-based annotations, or having it as part of a <code>@Bean</code> method inside an <code>@Configuration</code> class.</p>
</div>
<div class="paragraph">
<p>Another option is to have the repository itself implement the interface.</p>
</div>
<div class="exampleblock">
<div class="title">Example 13. Repository that provides the <code>QueryRewriter</code></div>
<div class="content">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">public interface MyRepository extends JpaRepository&lt;User, Long&gt;, QueryRewriter {

		@Query(value = "select original_user_alias.* from SD_USER original_user_alias",
                nativeQuery = true,
				queryRewriter = MyRepository.class)
		List&lt;User&gt; findByNativeQuery(String param);

		@Query(value = "select original_user_alias from User original_user_alias",
                queryRewriter = MyRepository.class)
		List&lt;User&gt; findByNonNativeQuery(String param);

		@Override
		default String rewrite(String query, Sort sort) {
			return query.replaceAll("original_user_alias", "rewritten_user_alias");
		}
}</code></pre>
</div>
</div>
</div>
</div>
<div class="paragraph">
<p>Depending on what you&#8217;re doing with your <code>QueryRewriter</code>, it may be advisable to have more than one, each registered with the application context.</p>
</div>
<div class="admonitionblock note">
<table>
<tr>
<td class="icon">
<i class="fa icon-note" title="Note"></i>
</td>
<td class="content">
In a CDI-based environment, Spring Data JPA will search the <code>BeanManager</code> for instances of your implementation of
<code>QueryRewriter</code>.
</td>
</tr>
</table>
</div>
</div>
</div>
</div>
<div class="sect1">
<h2 id="jpa.query-methods.sorting"><a class="anchor" href="#jpa.query-methods.sorting"></a>Using Sort</h2>
<div class="sectionbody">
<div class="paragraph">
<p>Sorting can be done by either providing a <code>PageRequest</code> or by using <code>Sort</code> directly. The properties actually used within the <code>Order</code> instances of <code>Sort</code> need to match your domain model, which means they need to resolve to either a property or an alias used within the query. The JPQL defines this as a state field path expression.</p>
</div>
<div class="admonitionblock note">
<table>
<tr>
<td class="icon">
<i class="fa icon-note" title="Note"></i>
</td>
<td class="content">
Using any non-referenceable path expression leads to an <code>Exception</code>.
</td>
</tr>
</table>
</div>
<div class="paragraph">
<p>However, using <code>Sort</code> together with <a href="#jpa.query-methods.at-query"><code>@Query</code></a> lets you sneak in non-path-checked <code>Order</code> instances containing functions within the <code>ORDER BY</code> clause. This is possible because the <code>Order</code> is appended to the given query string. By default, Spring Data JPA rejects any <code>Order</code> instance containing function calls, but you can use <code>JpaSort.unsafe</code> to add potentially unsafe ordering.</p>
</div>
<div class="paragraph">
<p>The following example uses <code>Sort</code> and <code>JpaSort</code>, including an unsafe option on <code>JpaSort</code>:</p>
</div>
<div class="exampleblock">
<div class="title">Example 14. Using <code>Sort</code> and <code>JpaSort</code></div>
<div class="content">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">public interface UserRepository extends JpaRepository&lt;User, Long&gt; {

  @Query("select u from User u where u.lastname like ?1%")
  List&lt;User&gt; findByAndSort(String lastname, Sort sort);

  @Query("select u.id, LENGTH(u.firstname) as fn_len from User u where u.lastname like ?1%")
  List&lt;Object[]&gt; findByAsArrayAndSort(String lastname, Sort sort);
}

repo.findByAndSort("lannister", Sort.by("firstname"));                <i class="conum" data-value="1"></i><b>(1)</b>
repo.findByAndSort("stark", Sort.by("LENGTH(firstname)"));            <i class="conum" data-value="2"></i><b>(2)</b>
repo.findByAndSort("targaryen", JpaSort.unsafe("LENGTH(firstname)")); <i class="conum" data-value="3"></i><b>(3)</b>
repo.findByAsArrayAndSort("bolton", Sort.by("fn_len"));               <i class="conum" data-value="4"></i><b>(4)</b></code></pre>
</div>
</div>
<div class="colist arabic">
<table>
<tr>
<td><i class="conum" data-value="1"></i><b>1</b></td>
<td>Valid <code>Sort</code> expression pointing to property in domain model.</td>
</tr>
<tr>
<td><i class="conum" data-value="2"></i><b>2</b></td>
<td>Invalid <code>Sort</code> containing function call.
Throws Exception.</td>
</tr>
<tr>
<td><i class="conum" data-value="3"></i><b>3</b></td>
<td>Valid <code>Sort</code> containing explicitly <em>unsafe</em> <code>Order</code>.</td>
</tr>
<tr>
<td><i class="conum" data-value="4"></i><b>4</b></td>
<td>Valid <code>Sort</code> expression pointing to aliased function.</td>
</tr>
</table>
</div>
</div>
</div>
<div class="sect2">
<h3 id="_jpasort_unsafe_limitations"><a class="anchor" href="#_jpasort_unsafe_limitations"></a>JpaSort.unsafe(…) limitations</h3>
<div class="paragraph">
<p><code>JpaSort.unsafe(…)</code> operates in two modes:</p>
</div>
<div class="ulist">
<ul>
<li>
<p>When used with derived Queries or String-based Queries, the order string is appended to the query.</p>
</li>
<li>
<p>When used with Query by Example or Specifications (that use <code>CriteriaQuery</code>), order expressions are parsed and added to the <code>CriteriaQuery</code> as expressions.</p>
</li>
<li>
<p><code>JpaSort.JpaOrder.withUnsafe(…)</code> creates a new <code>JpaSort</code> applying current direction, case-sensitivity, and null-handling the given properties.
Query expressions can contain function calls, various clauses (such as <code>CASE WHEN</code>, arithmetic expressions) or property paths.
Order translation does not support subquery expressions, <code>TREAT</code> and <code>CAST</code>.</p>
</li>
</ul>
</div>
</div>
</div>
</div>
<div class="sect1">
<h2 id="jpa.query-methods.scroll"><a class="anchor" href="#jpa.query-methods.scroll"></a>Scrolling Large Query Results</h2>
<div class="sectionbody">
<div class="paragraph">
<p>When working with large data sets, <a href="../repositories/query-methods-details.html#repositories.scrolling" class="xref page">scrolling</a> can help to process those results efficiently without loading all results into memory.</p>
</div>
<div class="paragraph">
<p>You have multiple options to consume large query results:</p>
</div>
<div class="olist arabic">
<ol class="arabic">
<li>
<p><a href="../repositories/query-methods-details.html#repositories.paging-and-sorting" class="xref page">Paging</a>.
You have learned in the previous chapter about <code>Pageable</code> and <code>PageRequest</code>.</p>
</li>
<li>
<p><a href="../repositories/query-methods-details.html#repositories.scrolling.offset" class="xref page">Offset-based scrolling</a>.
This is a lighter variant than paging because it does not require the total result count.</p>
</li>
<li>
<p><a href="../repositories/query-methods-details.html#repositories.scrolling.keyset" class="xref page">Keyset-based scrolling</a>.
This method avoids <a href="https://use-the-index-luke.com/no-offset">the shortcomings of offset-based result retrieval by leveraging database indexes</a>.</p>
</li>
</ol>
</div>
<div class="paragraph">
<p>Read more on <a href="../repositories/query-methods-details.html#repositories.scrolling.guidance" class="xref page">which method to use best</a> for your particular arrangement.</p>
</div>
<div class="paragraph">
<p>You can use the Scroll API with query methods, <a href="../repositories/query-by-example.html" class="xref page">Query-by-Example</a>, and <a href="../repositories/core-extensions.html#core.extensions.querydsl" class="xref page">Querydsl</a>.</p>
</div>
<div class="admonitionblock note">
<table>
<tr>
<td class="icon">
<i class="fa icon-note" title="Note"></i>
</td>
<td class="content">
Scrolling with String-based query methods is not yet supported.
Scrolling is also not supported using stored <code>@Procedure</code> query methods.
</td>
</tr>
</table>
</div>
</div>
</div>
<div class="sect1">
<h2 id="jpa.named-parameters"><a class="anchor" href="#jpa.named-parameters"></a>Using Named Parameters</h2>
<div class="sectionbody">
<div class="paragraph">
<p>By default, Spring Data JPA uses position-based parameter binding, as described in all the preceding examples.
This makes query methods a little error-prone when refactoring regarding the parameter position.
To solve this issue, you can use <code>@Param</code> annotation to give a method parameter a concrete name and bind the name in the query, as shown in the following example:</p>
</div>
<div class="exampleblock">
<div class="title">Example 15. Using named parameters</div>
<div class="content">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">public interface UserRepository extends JpaRepository&lt;User, Long&gt; {

  @Query("select u from User u where u.firstname = :firstname or u.lastname = :lastname")
  User findByLastnameOrFirstname(@Param("lastname") String lastname,
                                 @Param("firstname") String firstname);
}</code></pre>
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
The method parameters are switched according to their order in the defined query.
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
As of version 4, Spring fully supports Java 8’s parameter name discovery based on the <code>-parameters</code> compiler flag. By using this flag in your build as an alternative to debug information, you can omit the <code>@Param</code> annotation for named parameters.
</td>
</tr>
</table>
</div>
</div>
</div>
<div class="sect1">
<h2 id="jpa.query.spel-expressions"><a class="anchor" href="#jpa.query.spel-expressions"></a>Templated Queries and Expressions</h2>
<div class="sectionbody">
<div class="paragraph">
<p>We support the usage of restricted expressions in manually defined queries that are defined with <code>@Query</code>.
Upon the query being run, these expressions are evaluated against a predefined set of variables.</p>
</div>
<div class="admonitionblock note">
<table>
<tr>
<td class="icon">
<i class="fa icon-note" title="Note"></i>
</td>
<td class="content">
If you are not familiar with Value Expressions, please refer to <a href="value-expressions.html" class="xref page">Value Expressions Fundamentals</a> to learn about SpEL Expressions and Property Placeholders.
</td>
</tr>
</table>
</div>
<div class="paragraph">
<p>Spring Data JPA supports a template variable called <code>entityName</code>.
Its usage is <code>select x from #{#entityName} x</code>.
It inserts the <code>entityName</code> of the domain type associated with the given repository.
The <code>entityName</code> is resolved as follows:
* If the domain type has set the name property on the <code>@Entity</code> annotation, it is used.
* Otherwise, the simple class-name of the domain type is used.</p>
</div>
<div class="paragraph">
<p>The following example demonstrates one use case for the <code>#{#entityName}</code> expression in a query string where you want to define a repository interface with a query method and a manually defined query:</p>
</div>
<div class="exampleblock">
<div class="title">Example 16. Using SpEL expressions in repository query methods: entityName</div>
<div class="content">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Entity
public class User {

  @Id
  @GeneratedValue
  Long id;

  String lastname;
}

public interface UserRepository extends JpaRepository&lt;User,Long&gt; {

  @Query("select u from #{#entityName} u where u.lastname = ?1")
  List&lt;User&gt; findByLastname(String lastname);
}</code></pre>
</div>
</div>
</div>
</div>
<div class="paragraph">
<p>To avoid stating the actual entity name in the query string of a <code>@Query</code> annotation, you can use the <code>#{#entityName}</code> variable.</p>
</div>
<div class="admonitionblock note">
<table>
<tr>
<td class="icon">
<i class="fa icon-note" title="Note"></i>
</td>
<td class="content">
The <code>entityName</code> can be customized by using the <code>@Entity</code> annotation.
Customizations in <code>orm.xml</code> are not supported for the SpEL expressions.
</td>
</tr>
</table>
</div>
<div class="paragraph">
<p>Of course, you could have just used <code>User</code> in the query declaration directly, but that would require you to change the query as well.
The reference to <code>#entityName</code> picks up potential future remappings of the <code>User</code> class to a different entity name (for example, by using <code>@Entity(name = "MyUser")</code>.</p>
</div>
<div class="paragraph">
<p>Another use case for the <code>#{#entityName}</code> expression in a query string is if you want to define a generic repository interface with specialized repository interfaces for a concrete domain type.
To not repeat the definition of custom query methods on the concrete interfaces, you can use the entity name expression in the query string of the <code>@Query</code> annotation in the generic repository interface, as shown in the following example:</p>
</div>
<div class="exampleblock">
<div class="title">Example 17. Using SpEL expressions in Repository Query Methods: entityName with Inheritance</div>
<div class="content">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@MappedSuperclass
public abstract class AbstractMappedType {
  …
  String attribute;
}

@Entity
public class ConcreteType extends AbstractMappedType { … }

@NoRepositoryBean
public interface MappedTypeRepository&lt;T extends AbstractMappedType&gt;
  extends Repository&lt;T, Long&gt; {

  @Query("select t from #{#entityName} t where t.attribute = ?1")
  List&lt;T&gt; findAllByAttribute(String attribute);
}

public interface ConcreteRepository
  extends MappedTypeRepository&lt;ConcreteType&gt; { … }</code></pre>
</div>
</div>
</div>
</div>
<div class="paragraph">
<p>In the preceding example, the <code>MappedTypeRepository</code> interface is the common parent interface for a few domain types extending <code>AbstractMappedType</code>.
It also defines the generic <code>findAllByAttribute(…)</code> method, which can be used on instances of the specialized repository interfaces.
If you now invoke <code>findAllByAttribute(…)</code> on <code>ConcreteRepository</code>, the query becomes <code>select t from ConcreteType t where t.attribute = ?1</code>.</p>
</div>
<div class="paragraph">
<p>You can also use expressions to control method arguments.
In these expressions the entity name is not available, but the arguments are.
They can be accessed by name or index as demonstrated in the following example.</p>
</div>
<div class="exampleblock">
<div class="title">Example 18. Using Value Expressions in Repository Query Methods: Accessing Arguments</div>
<div class="content">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Query("select u from User u where u.firstname = ?1 and u.firstname=?#{[0]} and u.emailAddress = ?#{principal.emailAddress}")
List&lt;User&gt; findByFirstnameAndCurrentUserWithCustomQuery(String firstname);</code></pre>
</div>
</div>
</div>
</div>
<div class="paragraph">
<p>For <code>like</code>-conditions one often wants to append <code>%</code> to the beginning or the end of a String valued parameter.
This can be done by appending or prefixing a bind parameter marker or a SpEL expression with <code>%</code>.
Again the following example demonstrates this.</p>
</div>
<div class="exampleblock">
<div class="title">Example 19. Using Value Expressions in Repository Query Methods: Wildcard shortcut</div>
<div class="content">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Query("select u from User u where u.lastname like %:#{[0]}% and u.lastname like %:lastname%")
List&lt;User&gt; findByLastnameWithSpelExpression(@Param("lastname") String lastname);</code></pre>
</div>
</div>
</div>
</div>
<div class="paragraph">
<p>When using <code>like</code>-conditions with values that are coming from a not secure source the values should be sanitized so they can&#8217;t contain any wildcards and thereby allow attackers to select more data than they should be able to.
For this purpose the <code>escape(String)</code> method is made available in the SpEL context.
It prefixes all instances of <code>_</code> and <code>%</code> in the first argument with the single character from the second argument.
In combination with the <code>escape</code> clause of the <code>like</code> expression available in JPQL and standard SQL this allows easy cleaning of bind parameters.</p>
</div>
<div class="exampleblock">
<div class="title">Example 20. Using Value Expressions in Repository Query Methods: Sanitizing Input Values</div>
<div class="content">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Query("select u from User u where u.firstname like %?#{escape([0])}% escape ?#{escapeCharacter()}")
List&lt;User&gt; findContainingEscaped(String namePart);</code></pre>
</div>
</div>
</div>
</div>
<div class="paragraph">
<p>Given this method declaration in a repository interface <code>findContainingEscaped("Peter_")</code> will find <code>Peter_Parker</code> but not <code>Peter Parker</code>.
The escape character used can be configured by setting the <code>escapeCharacter</code> of the <code>@EnableJpaRepositories</code> annotation.
Note that the method <code>escape(String)</code> available in the SpEL context will only escape the SQL and JPQL standard wildcards <code>_</code> and <code>%</code>.
If the underlying database or the JPA implementation supports additional wildcards these will not get escaped.</p>
</div>
<div class="exampleblock">
<div class="title">Example 21. Using Value Expressions in Repository Query Methods: Configuration Properties</div>
<div class="content">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Query("select u from User u where u.applicationName = ?${spring.application.name:unknown}")
List&lt;User&gt; findContainingEscaped(String namePart);</code></pre>
</div>
</div>
</div>
</div>
<div class="paragraph">
<p>You can refer in your query methods also to configuration property names including fallbacks if you wish to resolve a property from <code>Environment</code> during runtime.
The property is being evaluated upon query execution.
Typically, property placeholders resolve to String-like values.</p>
</div>
</div>
</div>
<div class="sect1">
<h2 id="jpa.query.other-methods"><a class="anchor" href="#jpa.query.other-methods"></a>Other Methods</h2>
<div class="sectionbody">
<div class="paragraph">
<p>Spring Data JPA offers many ways to build queries.
But sometimes, your query may simply be too complicated for the techniques offered.
In that situation, consider:</p>
</div>
<div class="ulist">
<ul>
<li>
<p>If you haven&#8217;t already, simply write the query yourself using <a href="#jpa.query-methods.at-query"><code>@Query</code></a>.</p>
</li>
<li>
<p>If that doesn&#8217;t fit your needs, consider implementing a <a href="../repositories/custom-implementations.html" class="xref page">custom implementation</a>. This lets you register a method in your repository while leaving the implementation completely up to you. This gives you the ability to:</p>
<div class="ulist">
<ul>
<li>
<p>Talk directly to the <code>EntityManager</code> (writing pure HQL/JPQL/EQL/native SQL or using the <strong>Criteria API</strong>)</p>
</li>
<li>
<p>Leverage Spring Framework&#8217;s <code>JdbcTemplate</code> (native SQL)</p>
</li>
<li>
<p>Use another 3rd-party database toolkit.</p>
</li>
</ul>
</div>
</li>
<li>
<p>Another option is putting your query inside the database and then using either Spring Data JPA&#8217;s <a href="stored-procedures.html" class="xref page"><code>@StoredProcedure</code> annotation</a> or if it&#8217;s a database function using the <a href="#jpa.query-methods.at-query"><code>@Query</code> annotation</a> and invoking it with a <code>CALL</code>.</p>
</li>
</ul>
</div>
<div class="paragraph">
<p>These tactics may be most effective when you need maximum control of your query, while still letting Spring Data JPA provide resource management.</p>
</div>
</div>
</div>
<div class="sect1">
<h2 id="jpa.modifying-queries"><a class="anchor" href="#jpa.modifying-queries"></a>Modifying Queries</h2>
<div class="sectionbody">
<div class="paragraph">
<p>All the previous sections describe how to declare queries to access a given entity or collection of entities.
You can add custom modifying behavior by using the custom method facilities described in <a href="https://docs.spring.io/spring-data/commons/reference/4.1/repositories/custom-implementations.html">Custom Implementations for Spring Data Repositories</a>.
As this approach is feasible for comprehensive custom functionality, you can modify queries that only need parameter binding by annotating the query method with <code>@Modifying</code>, as shown in the following example:</p>
</div>
<div class="exampleblock">
<div class="title">Example 22. Declaring manipulating queries</div>
<div class="content">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Modifying
@Query("update User u set u.firstname = ?1 where u.lastname = ?2")
int setFixedFirstnameFor(String firstname, String lastname);</code></pre>
</div>
</div>
</div>
</div>
<div class="paragraph">
<p>Doing so triggers the query annotated to the method as an updating query instead of a selecting one. As the <code>EntityManager</code> might contain outdated entities after the execution of the modifying query, we do not automatically clear it (see the <a href="https://jakarta.ee/specifications/persistence/3.2/apidocs/jakarta.persistence/jakarta/persistence/entitymanager">JavaDoc</a> of <code>EntityManager.clear()</code> for details), since this effectively drops all non-flushed changes still pending in the <code>EntityManager</code>.
If you wish the <code>EntityManager</code> to be cleared automatically, you can set the <code>@Modifying</code> annotation&#8217;s <code>clearAutomatically</code> attribute to <code>true</code>.</p>
</div>
<div class="paragraph">
<p>The <code>@Modifying</code> annotation is only relevant in combination with the <code>@Query</code> annotation.
Derived query methods or custom methods do not require this annotation.</p>
</div>
<div class="sect2">
<h3 id="jpa.modifying-queries.derived-delete"><a class="anchor" href="#jpa.modifying-queries.derived-delete"></a>Derived Delete Queries</h3>
<div class="paragraph">
<p>Spring Data JPA also supports derived delete queries that let you avoid having to declare the JPQL query explicitly, as shown in the following example:</p>
</div>
<div class="exampleblock">
<div class="title">Example 23. Using a derived delete query</div>
<div class="content">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">interface UserRepository extends Repository&lt;User, Long&gt; {

  void deleteByRoleId(long roleId);

  @Modifying
  @Query("delete from User u where u.role.id = ?1")
  void deleteInBulkByRoleId(long roleId);
}</code></pre>
</div>
</div>
</div>
</div>
<div class="paragraph">
<p>Although the <code>deleteByRoleId(…)</code> method looks like it basically produces the same result as the <code>deleteInBulkByRoleId(…)</code>, there is an important difference between the two method declarations in terms of the way they are run.
As the name suggests, the latter method issues a single JPQL query (the one defined in the annotation) against the database.
This means even currently loaded instances of <code>User</code> do not see lifecycle callbacks invoked.</p>
</div>
<div class="paragraph">
<p>To make sure lifecycle queries are actually invoked, an invocation of <code>deleteByRoleId(…)</code> runs a query and then deletes the returned instances one by one, so that the persistence provider can actually invoke <code>@PreRemove</code> callbacks on those entities.</p>
</div>
<div class="paragraph">
<p>In fact, a derived delete query is a shortcut for running the query and then calling <code>CrudRepository.delete(Iterable&lt;User&gt; users)</code> on the result and keeping behavior in sync with the implementations of other <code>delete(…)</code> methods in <code>CrudRepository</code>.</p>
</div>
<div class="admonitionblock note">
<table>
<tr>
<td class="icon">
<i class="fa icon-note" title="Note"></i>
</td>
<td class="content">
When deleting a lot of objects you will need to consider the performance implications to ensure sufficient memory availability.
All resulting objects are loaded into memory before being deleted and are held in the session until flushing or completing the transaction.
</td>
</tr>
</table>
</div>
</div>
</div>
</div>
<div class="sect1">
<h2 id="jpa.query-hints"><a class="anchor" href="#jpa.query-hints"></a>Applying Query Hints</h2>
<div class="sectionbody">
<div class="paragraph">
<p>To apply JPA query hints to the queries declared in your repository interface, you can use the <code>@QueryHints</code> annotation.
It takes an array of JPA <code>@QueryHint</code> annotations plus a boolean flag to potentially disable the hints applied to the additional count query triggered when applying pagination, as shown in the following example:</p>
</div>
<div class="exampleblock">
<div class="title">Example 24. Using QueryHints with a repository method</div>
<div class="content">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">public interface UserRepository extends Repository&lt;User, Long&gt; {

  @QueryHints(value = { @QueryHint(name = "name", value = "value")},
              forCounting = false)
  Page&lt;User&gt; findByLastname(String lastname, Pageable pageable);
}</code></pre>
</div>
</div>
</div>
</div>
<div class="paragraph">
<p>The preceding declaration would apply the configured <code>@QueryHint</code> for the actual query but omit applying it to the count query triggered to calculate the total number of pages.</p>
</div>
<div class="admonitionblock note">
<table>
<tr>
<td class="icon">
<i class="fa icon-note" title="Note"></i>
</td>
<td class="content">
When using Java <code>Stream&lt;T&gt;</code>, review the <a href="../repositories/query-return-types-reference.html#return-type.stream" class="xref page"><code>Stream</code> semantics and resource handling</a> in the context of your JPA provider and JDBC driver considering specifically provider-specific fetch behavior and driver fetch-size settings.
</td>
</tr>
</table>
</div>
<div class="sect2">
<h3 id="jpa.query-hints.comments"><a class="anchor" href="#jpa.query-hints.comments"></a>Adding Comments to Queries</h3>
<div class="paragraph">
<p>Sometimes, you need to debug a query based upon database performance.
The query your database administrator shows you may look VERY different than what you wrote using <code>@Query</code>, or it may look
nothing like what you presume Spring Data JPA has generated regarding a custom finder or if you used query by example.</p>
</div>
<div class="paragraph">
<p>To make this process easier, you can insert custom comments into almost any JPA operation, whether its a query or other operation
by applying the <code>@Meta</code> annotation.</p>
</div>
<div class="exampleblock">
<div class="title">Example 25. Apply <code>@Meta</code> annotation to repository operations</div>
<div class="content">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">public interface RoleRepository extends JpaRepository&lt;Role, Integer&gt; {

	@Meta(comment = "find roles by name")
	List&lt;Role&gt; findByName(String name);

	@Override
	@Meta(comment = "find roles using QBE")
	&lt;S extends Role&gt; List&lt;S&gt; findAll(Example&lt;S&gt; example);

	@Meta(comment = "count roles for a given name")
	long countByName(String name);

	@Override
	@Meta(comment = "exists based on QBE")
	&lt;S extends Role&gt; boolean exists(Example&lt;S&gt; example);
}</code></pre>
</div>
</div>
</div>
</div>
<div class="paragraph">
<p>This sample repository has a mixture of custom finders as well as overriding the inherited operations from <code>JpaRepository</code>.
Either way, the <code>@Meta</code> annotation lets you add a <code>comment</code> that will be inserted into queries before they are sent to the database.</p>
</div>
<div class="paragraph">
<p>It&#8217;s also important to note that this feature isn&#8217;t confined solely to queries. It extends to the <code>count</code> and <code>exists</code> operations.
And while not shown, it also extends to certain <code>delete</code> operations.</p>
</div>
<div class="admonitionblock important">
<table>
<tr>
<td class="icon">
<i class="fa icon-important" title="Important"></i>
</td>
<td class="content">
While we have attempted to apply this feature everywhere possible, some operations of the underlying <code>EntityManager</code> don&#8217;t support comments. For example, <code>entityManager.createQuery()</code> is clearly documented as supporting comments, but <code>entityManager.find()</code> operations do not.
</td>
</tr>
</table>
</div>
<div class="paragraph">
<p>Neither JPQL logging nor SQL logging is a standard in JPA, so each provider requires custom configuration, as shown the sections below.</p>
</div>
<div class="sect3">
<h4 id="activating-hibernate-comments"><a class="anchor" href="#activating-hibernate-comments"></a>Activating Hibernate comments</h4>
<div class="paragraph">
<p>To activate query comments in Hibernate, you must set <code>hibernate.use_sql_comments</code> to <code>true</code>.</p>
</div>
<div class="paragraph">
<p>If you are using Java-based configuration settings, this can be done like this:</p>
</div>
<div class="exampleblock">
<div class="title">Example 26. Java-based JPA configuration</div>
<div class="content">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Bean
public Properties jpaProperties() {

	Properties properties = new Properties();
	properties.setProperty("hibernate.use_sql_comments", "true");
	return properties;
}</code></pre>
</div>
</div>
</div>
</div>
<div class="paragraph">
<p>If you have a <code>persistence.xml</code> file, you can apply it there:</p>
</div>
<div class="exampleblock">
<div class="title">Example 27. <code>persistence.xml</code>-based configuration</div>
<div class="content">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-xml hljs" data-lang="xml">&lt;persistence-unit name="my-persistence-unit"&gt;

   ...registered classes...

	&lt;properties&gt;
		&lt;property name="hibernate.use_sql_comments" value="true" /&gt;
	&lt;/properties&gt;
&lt;/persistence-unit&gt;</code></pre>
</div>
</div>
</div>
</div>
<div class="paragraph">
<p>Finally, if you are using Spring Boot, then you can set it up inside your <code>application.properties</code> file:</p>
</div>
<div class="exampleblock">
<div class="title">Example 28. Spring Boot property-based configuration</div>
<div class="content">
<div class="listingblock">
<div class="content">
<pre>spring.jpa.properties.hibernate.use_sql_comments=true</pre>
</div>
</div>
</div>
</div>
</div>
<div class="sect3">
<h4 id="activating-eclipselink-comments"><a class="anchor" href="#activating-eclipselink-comments"></a>Activating EclipseLink comments</h4>
<div class="paragraph">
<p>To activate query comments in EclipseLink, you must set <code>eclipselink.logging.level.sql</code> to <code>FINE</code>.</p>
</div>
<div class="paragraph">
<p>If you are using Java-based configuration settings, this can be done like this:</p>
</div>
<div class="exampleblock">
<div class="title">Example 29. Java-based JPA configuration</div>
<div class="content">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Bean
public Properties jpaProperties() {

	Properties properties = new Properties();
	properties.setProperty("eclipselink.logging.level.sql", "FINE");
	return properties;
}</code></pre>
</div>
</div>
</div>
</div>
<div class="paragraph">
<p>If you have a <code>persistence.xml</code> file, you can apply it there:</p>
</div>
<div class="exampleblock">
<div class="title">Example 30. <code>persistence.xml</code>-based configuration</div>
<div class="content">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-xml hljs" data-lang="xml">&lt;persistence-unit name="my-persistence-unit"&gt;

   ...registered classes...

	&lt;properties&gt;
		&lt;property name="eclipselink.logging.level.sql" value="FINE" /&gt;
	&lt;/properties&gt;
&lt;/persistence-unit&gt;</code></pre>
</div>
</div>
</div>
</div>
<div class="paragraph">
<p>Finally, if you are using Spring Boot, then you can set it up inside your <code>application.properties</code> file:</p>
</div>
<div class="exampleblock">
<div class="title">Example 31. Spring Boot property-based configuration</div>
<div class="content">
<div class="listingblock">
<div class="content">
<pre>spring.jpa.properties.eclipselink.logging.level.sql=FINE</pre>
</div>
</div>
</div>
</div>
</div>
</div>
</div>
</div>
<div class="sect1">
<h2 id="jpa.entity-graph"><a class="anchor" href="#jpa.entity-graph"></a>Configuring Fetch- and LoadGraphs</h2>
<div class="sectionbody">
<div class="paragraph">
<p>The JPA 2.1 specification introduced support for specifying Fetch- and LoadGraphs that we also support with the <code>@EntityGraph</code> annotation, which lets you reference a <code>@NamedEntityGraph</code> definition. You can use that annotation on an entity to configure the fetch plan of the resulting query. The type (<code>Fetch</code> or <code>Load</code>) of the fetching can be configured by using the <code>type</code> attribute on the <code>@EntityGraph</code> annotation. See the JPA 2.1 Spec 3.7.4 for further reference.</p>
</div>
<div class="paragraph">
<p>The following example shows how to define a named entity graph on an entity:</p>
</div>
<div class="exampleblock">
<div class="title">Example 32. Defining a named entity graph on an entity.</div>
<div class="content">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Entity
@NamedEntityGraph(name = "GroupInfo.detail",
  attributeNodes = @NamedAttributeNode("members"))
public class GroupInfo {

  // default fetch mode is lazy.
  @ManyToMany
  List&lt;GroupMember&gt; members = new ArrayList&lt;GroupMember&gt;();

  …
}</code></pre>
</div>
</div>
</div>
</div>
<div class="paragraph">
<p>The following example shows how to reference a named entity graph on a repository query method:</p>
</div>
<div class="exampleblock">
<div class="title">Example 33. Referencing a named entity graph definition on a repository query method.</div>
<div class="content">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">public interface GroupRepository extends CrudRepository&lt;GroupInfo, String&gt; {

  @EntityGraph(value = "GroupInfo.detail", type = EntityGraphType.LOAD)
  GroupInfo getByGroupName(String name);

}</code></pre>
</div>
</div>
</div>
</div>
<div class="paragraph">
<p>It is also possible to define ad hoc entity graphs by using <code>@EntityGraph</code>. The provided <code>attributePaths</code> are translated into the according <code>EntityGraph</code> without needing to explicitly add <code>@NamedEntityGraph</code> to your domain types, as shown in the following example:</p>
</div>
<div class="exampleblock">
<div class="title">Example 34. Using ad-hoc entity graph definitions on a repository query method</div>
<div class="content">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">public interface GroupRepository extends CrudRepository&lt;GroupInfo, String&gt; {

  @EntityGraph(attributePaths = { "members" })
  GroupInfo getByGroupName(String name);

}</code></pre>
</div>
</div>
</div>
</div>
</div>
</div>
<div class="sect1">
<h2 id="repositories.scrolling"><a class="anchor" href="#repositories.scrolling"></a>Scrolling</h2>
<div class="sectionbody">
<div class="paragraph">
<p>Scrolling is a more fine-grained approach to iterating through chunks of larger result sets.
Scrolling consists of a stable sort, a scroll type (Offset- or Keyset-based scrolling) and result limiting.
You can define simple sorting expressions by using property names and define static result limiting using the <a href="../repositories/query-methods-details.html#repositories.limit-query-result" class="xref page"><code>Top</code> or <code>First</code> keyword</a> through query derivation.
You can concatenate expressions to collect multiple criteria into one expression.</p>
</div>
<div class="paragraph">
<p>Scroll queries return a <code>Window&lt;T&gt;</code> that allows obtaining the element&#8217;s scroll position to fetch the next <code>Window&lt;T&gt;</code> until your application has consumed the entire query result.
Similar to consuming a Java <code>Iterator&lt;List&lt;…&gt;&gt;</code> by obtaining the next batch of results, query result scrolling lets you access a <code>ScrollPosition</code> through <code>Window.positionAt(…)</code>, as in the following example:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">Window&lt;User&gt; users = repository.findFirst10ByLastnameOrderByFirstname("Doe", ScrollPosition.offset());
do {

  for (User u : users) {
    // consume the user
  }

  if (users.isLast() || users.isEmpty()) {
    break;
  }

  // obtain the next Scroll
  users = repository.findFirst10ByLastnameOrderByFirstname("Doe", users.positionAt(users.size() - 1));
} while (!users.isEmpty());</code></pre>
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
<p>The <code>ScrollPosition</code> identifies the exact position of an element with the entire query result.
Query execution treats the position parameter <em>exclusive</em>, results will start <em>after</em> the given position.
<code>ScrollPosition#offset()</code> and <code>ScrollPosition#keyset()</code> as special incarnations of a <code>ScrollPosition</code> indicating the start of a scroll operation.</p>
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
<p>The above example shows static sorting and limiting.
You can define query methods alternatively that accept a <code>Sort</code> object define a more complex sorting order or sorting on a per-request basis.
In a similar way, providing a <code>Limit</code> object allows you to define a dynamic limit on a per-request basis instead of applying a static limitation.
Read more on dynamic sorting and limiting in the <a href="../repositories/query-methods-details.html#repositories.special-parameters" class="xref page">Query Methods Details</a>.</p>
</div>
</td>
</tr>
</table>
</div>
<div class="paragraph">
<p>Scrolling through consuming <code>Window</code> instances requires quite a few conditionals to reach optimum database round-trips and can quickly become a repetitive task that can be simplified using <code>WindowIterator</code>.</p>
</div>
<div class="paragraph">
<p><code>WindowIterator</code> provides a utility to simplify scrolling across <code>Window</code>s by removing the need to check for the presence of a next <code>Window</code> and applying the <code>ScrollPosition</code>.</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">WindowIterator&lt;User&gt; users = WindowIterator.of(position -&gt; repository.findFirst10ByLastnameOrderByFirstname("Doe", position))
  .startingAt(ScrollPosition.offset());

while (users.hasNext()) {
  User u = users.next();
  // consume the user
}</code></pre>
</div>
</div>
<div class="sect2">
<h3 id="repositories.scrolling.offset"><a class="anchor" href="#repositories.scrolling.offset"></a>Scrolling using Offset</h3>
<div class="paragraph">
<p>Offset scrolling uses similar to pagination, an Offset counter to skip a number of results and let the data source only return results beginning at the given Offset.
This simple mechanism avoids large results being sent to the client application.
However, most databases require materializing the full query result before your server can return the results.</p>
</div>
<div class="exampleblock">
<div class="title">Example 35. Using <code>OffsetScrollPosition</code> with Repository Query Methods</div>
<div class="content">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">interface UserRepository extends Repository&lt;User, Long&gt; {

  Window&lt;User&gt; findFirst10ByLastnameOrderByFirstname(String lastname, OffsetScrollPosition position);
}

WindowIterator&lt;User&gt; users = WindowIterator.of(position -&gt; repository.findFirst10ByLastnameOrderByFirstname("Doe", position))
  .startingAt(OffsetScrollPosition.initial()); <i class="conum" data-value="1"></i><b>(1)</b></code></pre>
</div>
</div>
<div class="colist arabic">
<table>
<tr>
<td><i class="conum" data-value="1"></i><b>1</b></td>
<td>Start with no offset to include the element at position <code>0</code>.</td>
</tr>
</table>
</div>
</div>
</div>
<div class="admonitionblock caution">
<table>
<tr>
<td class="icon">
<i class="fa icon-caution" title="Caution"></i>
</td>
<td class="content">
<div class="paragraph">
<p>There is a difference between <code>ScrollPosition.offset()</code> and <code>ScrollPosition.offset(0L)</code>.
The former indicates the start of scroll operation, pointing to no specific offset whereas the latter identifies the first element (at position <code>0</code>) of the result.
Given the <em>exclusive</em> nature of scrolling, using <code>ScrollPosition.offset(0)</code> skips the first element and translate to an offset of <code>1</code>.</p>
</div>
</td>
</tr>
</table>
</div>
</div>
<div class="sect2">
<h3 id="repositories.scrolling.keyset"><a class="anchor" href="#repositories.scrolling.keyset"></a>Scrolling using Keyset-Filtering</h3>
<div class="paragraph">
<p>Offset-based scrolling requires most databases to materialize the entire result before the server can return it.
So while the client only sees the portion of the requested results, your server needs to build the full result, which causes additional load.</p>
</div>
<div class="paragraph">
<p>Keyset-Filtering approaches result subset retrieval by leveraging built-in capabilities of your database aiming to reduce the computation and I/O requirements for individual queries.
This approach maintains a set of keys to resume scrolling by passing keys into the query, effectively amending your filter criteria.</p>
</div>
<div class="paragraph">
<p>The core idea of Keyset-Filtering is to start retrieving results using a stable sorting order.
Once you want to scroll to the next chunk, you obtain a <code>ScrollPosition</code> that is used to reconstruct the position within the sorted result.
The <code>ScrollPosition</code> captures the keyset of the last entity within the current <code>Window</code>.
To run the query, reconstruction rewrites the criteria clause to include all sort fields and the primary key so that the database can leverage potential indexes to run the query.
The database needs only constructing a much smaller result from the given keyset position without the need to fully materialize a large result and then skipping results until reaching a particular offset.</p>
</div>
<div class="admonitionblock warning">
<table>
<tr>
<td class="icon">
<i class="fa icon-warning" title="Warning"></i>
</td>
<td class="content">
<div class="paragraph">
<p>Keyset-Filtering requires the keyset properties (those used for sorting) to be non-nullable.
This limitation applies due to the store specific <code>null</code> value handling of comparison operators as well as the need to run queries against an indexed source.
Keyset-Filtering on nullable properties will lead to unexpected results.</p>
</div>
</td>
</tr>
</table>
</div>
<div class="listingblock">
<div class="title">Using <code>KeysetScrollPosition</code> with Repository Query Methods</div>
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">interface UserRepository extends Repository&lt;User, Long&gt; {

  Window&lt;User&gt; findFirst10ByLastnameOrderByFirstname(String lastname, KeysetScrollPosition position);
}

WindowIterator&lt;User&gt; users = WindowIterator.of(position -&gt; repository.findFirst10ByLastnameOrderByFirstname("Doe", position))
  .startingAt(ScrollPosition.keyset()); <i class="conum" data-value="1"></i><b>(1)</b></code></pre>
</div>
</div>
<div class="colist arabic">
<table>
<tr>
<td><i class="conum" data-value="1"></i><b>1</b></td>
<td>Start at the very beginning and do not apply additional filtering.</td>
</tr>
</table>
</div>
<div class="paragraph">
<p>Keyset-Filtering works best when your database contains an index that matches the sort fields, hence a static sort works well.
Scroll queries applying Keyset-Filtering require to the properties used in the sort order to be returned by the query, and these must be mapped in the returned entity.</p>
</div>
<div class="paragraph">
<p>You can use interface and DTO projections, however make sure to include all properties that you&#8217;ve sorted by to avoid keyset extraction failures.</p>
</div>
<div class="paragraph">
<p>When specifying your <code>Sort</code> order, it is sufficient to include sort properties relevant to your query;
You do not need to ensure unique query results if you do not want to.
The keyset query mechanism amends your sort order by including the primary key (or any remainder of composite primary keys) to ensure each query result is unique.</p>
</div>
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
                          <a class="title" href="../data-commons/index.html">Spring Data Commons</a>
                        </div>                        <div class="version-item is-active">
                          <div>
                            <button class="version-toggle" type="button">
                              <span></span>
                              Stable
                            </button>
                          </div>
                          <ul class="versions">
  <li class="version">
    <a href="../data-commons/index.html">
      4.1.1
    </a>
  </li>
  <li class="version">
    <a href="../data-commons/4.0/index.html">
      4.0.7
    </a>
  </li>
  <li class="version">
    <a href="../data-commons/3.5/index.html">
      3.5.13
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
    <a href="../data-commons/4.2/index.html">
      4.2.0-M1
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
    <a href="../data-commons/4.2-SNAPSHOT/index.html">
      4.2.0-SNAPSHOT
    </a>
  </li>
  <li class="version">
    <a href="../data-commons/4.1-SNAPSHOT/index.html">
      4.1.2-SNAPSHOT
    </a>
  </li>
  <li class="version">
    <a href="../data-commons/4.0-SNAPSHOT/index.html">
      4.0.8-SNAPSHOT
    </a>
  </li>
  <li class="version">
    <a href="../data-commons/3.5-SNAPSHOT/index.html">
      3.5.14-SNAPSHOT
    </a>
  </li>
</ul>                        </div>
                        
                      </li>
                      <li class="component">
                        <div>
                          <a class="title" href="../index.html">Spring Data JPA</a>
                        </div>                        <div class="version-item is-active">
                          <div>
                            <button class="version-toggle" type="button">
                              <span></span>
                              Stable
                            </button>
                          </div>
                          <ul class="versions">
  <li class="version">
    <a href="query-methods.html">
      4.1.1
    </a>
  </li>
  <li class="version">
    <a href="../4.0/jpa/query-methods.html">
      4.0.7
    </a>
  </li>
  <li class="version">
    <a href="../3.5/jpa/query-methods.html">
      3.5.13
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
    <a href="../4.2/jpa/query-methods.html">
      4.2.0-M1
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
    <a href="../4.2-SNAPSHOT/jpa/query-methods.html">
      4.2.0-SNAPSHOT
    </a>
  </li>
  <li class="version">
    <a href="../4.1-SNAPSHOT/jpa/query-methods.html">
      4.1.2-SNAPSHOT
    </a>
  </li>
  <li class="version">
    <a href="../4.0-SNAPSHOT/jpa/query-methods.html">
      4.0.8-SNAPSHOT
    </a>
  </li>
  <li class="version">
    <a href="../3.5-SNAPSHOT/jpa/query-methods.html">
      3.5.14-SNAPSHOT
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
  Spring Data
<ul>
    <li>
<a href="https://docs.spring.io/spring-data/cassandra/reference/">
  Spring Data Cassandra
</a>
</li>
    <li>
<a href="https://docs.spring.io/spring-data/commons/reference/">
  Spring Data Commons
</a>
</li>
    <li>
<a href="https://docs.spring.io/spring-data/couchbase/reference/">
  Spring Data Couchbase
</a>
</li>
    <li>
<a href="https://docs.spring.io/spring-data/elasticsearch/reference/">
  Spring Data Elasticsearch
</a>
</li>
    <li>
<a href="https://docs.spring.io/spring-data/jpa/reference/">
  Spring Data JPA
</a>
</li>
    <li>
<a href="https://docs.spring.io/spring-data/keyvalue/reference/">
  Spring Data KeyValue
</a>
</li>
    <li>
<a href="https://docs.spring.io/spring-data/ldap/reference/">
  Spring Data LDAP
</a>
</li>
    <li>
<a href="https://docs.spring.io/spring-data/mongodb/reference/">
  Spring Data MongoDB
</a>
</li>
    <li>
<a href="https://docs.spring.io/spring-data/neo4j/reference/">
  Spring Data Neo4j
</a>
</li>
    <li>
<a href="https://docs.spring.io/spring-data/redis/reference/">
  Spring Data Redis
</a>
</li>
    <li>
<a href="https://docs.spring.io/spring-data/relational/reference/">
  Spring Data JDBC &amp; R2DBC
</a>
</li>
    <li>
<a href="https://docs.spring.io/spring-data/rest/reference/">
  Spring Data REST
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
  <li><a href="../spring-projects.html">All Docs...</a></li>
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
        <img id="springlogo" src="../_/img/spring-logo.svg" alt="Spring">
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
<script src="../_/js/vendor/import.js"></script>
<script src="../_/js/site.js"></script>
<script async src="../_/js/vendor/highlight.js"></script>
<script async src="../_/js/vendor/asciidoctor-tabs.js" data-sync-storage-key="docs:preferred-tab"></script>

<div class="modal micromodal-slide" id="modal-1" aria-hidden="true">
    <div class="modal__overlay" tabindex="-1" data-micromodal-close>
        <div class="modal__container" role="dialog" aria-modal="true" aria-labelledby="modal-1-title">
            <main class="modal__content" id="modal-1-content">
                <div id="searchbox"></div>
                <div id="counter"></div>
                <div class="search-link-box">
                    <a class="search-link" href="../search.html">Search in all Spring Docs</a>
                </div>
                <div class="search-by">
                    <a target="_blank" rel="noopener noreferrer" href="https://www.algolia.com/" aria-label="Search by Algolia">
                        <img class="light" width="140" src="../_/img/algolia-light.svg" />
                        <img class="dark" width="140" src="../_/img/algolia-dark.svg" />
                    </a>
                </div>
                <div id="hits"></div>
            </main>
        </div>
    </div>
</div>

<script src="../_/js/vendor/hotkeys.js"></script>
<script src="https://cdn.jsdelivr.net/npm/algoliasearch@4.17.0/dist/algoliasearch-lite.umd.js" integrity="sha256-Lf9DrpGmcRip6OQzbcL6lnvNmoZNSKpyQX5pMlwatWE=" crossorigin="anonymous"></script>
<script src="https://cdn.jsdelivr.net/npm/instantsearch.js@4.54.1/dist/instantsearch.production.min.js" integrity="sha256-xYsZPDeNjYNTBWLvqD2Lxe98hOxcDgOHyMPfz4tVAbk=" crossorigin="anonymous"></script>
<script async id="search-script" src="../_/js/vendor/search.js" data-app-id="WB1FQYI187" data-api-key="c2e84f15fa630d534f1c62b1c413bb77" data-index-name="springdocs" data-stylesheet="../_/css/vendor/search.css" data-page-version="4.1.1" data-page-generation="4.1" data-page-component="data-jpa"></script>
  </body>
</html>

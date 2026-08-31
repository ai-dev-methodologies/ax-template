<!DOCTYPE html>
<html lang="en">
  <script src="https://cdn.cookielaw.org/scripttemplates/otSDKStub.js" data-domain-script="018ee325-b3a7-7753-937b-b8b3e643b1a7"></script><script>function OptanonWrapper() {}</script><script>function setGTM(w, d, s, l, i) { w[l] = w[l] || []; w[l].push({ "gtm.start": new Date().getTime(), event: "gtm.js"}); var f = d.getElementsByTagName(s)[0], j = d.createElement(s), dl = l != "dataLayer" ? "&l=" + l : ""; j.async = true; j.src = "https://www.googletagmanager.com/gtm.js?id=" + i + dl; f.parentNode.insertBefore(j, f); } if (document.cookie.indexOf("OptanonConsent") > -1 && document.cookie.indexOf("groups=") > -1) { setGTM(window, document, "script", "dataLayer", "GTM-W8CQ8TL"); } else { waitForOnetrustActiveGroups(); } var timer; function waitForOnetrustActiveGroups() { if (document.cookie.indexOf("OptanonConsent") > -1 && document.cookie.indexOf("groups=") > -1) { clearTimeout(timer); setGTM(window, document, "script", "dataLayer", "GTM-W8CQ8TL"); } else { timer = setTimeout(waitForOnetrustActiveGroups, 250); }}</script>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>Declarative Annotation-based Caching :: Spring Framework</title>
    <link rel="canonical" href="https://docs.spring.io/spring-framework/reference/integration/cache/annotations.html">
    <link rel="prev" href="strategies.html">
    <link rel="next" href="jsr-107.html">
    <meta name="generator" content="Antora 3.2.0-alpha.12">
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
    <meta name="version" content="7.0.9">
    <meta name="generation" content="7.0">
    <meta name="versioned-url" content="https://docs.spring.io/spring-framework/reference/7.0/integration/cache/annotations.html">
    <meta name="component" content="framework">
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
<div class="nav-container" data-component="framework" data-version="7.0.9">
  <aside class="nav">
    <div class="panels">
      <div class="nav-panel-menu is-active" data-panel="menu">
        <nav class="nav-menu">
<div class="context">
  <span class="title">Spring Framework</span>
  <span class="version">7.0.9</span>
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
    <a class="nav-link"  href="../../overview.html">Overview</a>
  </li>
  <li class="nav-item" data-depth="1">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../../core.html">Core Technologies</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../../core/beans.html">The IoC Container</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../core/beans/introduction.html">Introduction to the Spring IoC Container and Beans</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../core/beans/basics.html">Container Overview</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../core/beans/definition.html">Bean Overview</a>
  </li>
  <li class="nav-item" data-depth="3">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../../core/beans/dependencies.html">Dependencies</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../core/beans/dependencies/factory-collaborators.html">Dependency Injection</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../core/beans/dependencies/factory-properties-detailed.html">Dependencies and Configuration in Detail</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../core/beans/dependencies/factory-dependson.html">Using <code>depends-on</code></a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../core/beans/dependencies/factory-lazy-init.html">Lazy-initialized Beans</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../core/beans/dependencies/factory-autowire.html">Autowiring Collaborators</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../core/beans/dependencies/factory-method-injection.html">Method Injection</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../core/beans/factory-scopes.html">Bean Scopes</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../core/beans/factory-nature.html">Customizing the Nature of a Bean</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../core/beans/child-bean-definitions.html">Bean Definition Inheritance</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../core/beans/factory-extension.html">Container Extension Points</a>
  </li>
  <li class="nav-item" data-depth="3">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../../core/beans/annotation-config.html">Annotation-based Container Configuration</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../core/beans/annotation-config/autowired.html">Using <code>@Autowired</code></a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../core/beans/annotation-config/autowired-primary.html">Fine-tuning Annotation-based Autowiring with <code>@Primary</code> or <code>@Fallback</code></a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../core/beans/annotation-config/autowired-qualifiers.html">Fine-tuning Annotation-based Autowiring with Qualifiers</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../core/beans/annotation-config/generics-as-qualifiers.html">Using Generics as Autowiring Qualifiers</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../core/beans/annotation-config/custom-autowire-configurer.html">Using <code>CustomAutowireConfigurer</code></a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../core/beans/annotation-config/resource.html">Injection with <code>@Resource</code></a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../core/beans/annotation-config/value-annotations.html">Using <code>@Value</code></a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../core/beans/annotation-config/postconstruct-and-predestroy-annotations.html">Using <code>@PostConstruct</code> and <code>@PreDestroy</code></a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../core/beans/classpath-scanning.html">Classpath Scanning and Managed Components</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../core/beans/standard-annotations.html">Using JSR-330 Standard Annotations</a>
  </li>
  <li class="nav-item" data-depth="3">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../../core/beans/java.html">Java-based Container Configuration</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../core/beans/java/basic-concepts.html">Basic Concepts: <code>@Bean</code> and <code>@Configuration</code></a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../core/beans/java/instantiating-container.html">Instantiating the Spring Container by Using <code>AnnotationConfigApplicationContext</code></a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../core/beans/java/bean-annotation.html">Using the <code>@Bean</code> Annotation</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../core/beans/java/configuration-annotation.html">Using the <code>@Configuration</code> annotation</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../core/beans/java/composing-configuration-classes.html">Composing Java-based Configurations</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../core/beans/java/programmatic-bean-registration.html">Programmatic Bean Registration</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../core/beans/environment.html">Environment Abstraction</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../core/beans/context-load-time-weaver.html">Registering a <code>LoadTimeWeaver</code></a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../core/beans/context-introduction.html">Additional Capabilities of the <code>ApplicationContext</code></a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../core/beans/beanfactory.html">The <code>BeanFactory</code> API</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../../core/resources.html">Resources</a>
  </li>
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../../core/validation.html">Validation, Data Binding, and Type Conversion</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../core/validation/validator.html">Validation Using Spring&#8217;s Validator Interface</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../core/validation/data-binding.html">Data Binding</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../core/validation/error-code-resolution.html">Resolving Error Codes to Error Messages</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../core/validation/convert.html">Spring Type Conversion</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../core/validation/format.html">Spring Field Formatting</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../core/validation/format-configuring-formatting-globaldatetimeformat.html">Configuring a Global Date and Time Format</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../core/validation/beanvalidation.html">Java Bean Validation</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../../core/expressions.html">Spring Expression Language (SpEL)</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../core/expressions/evaluation.html">Evaluation</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../core/expressions/beandef.html">Expressions in Bean Definitions</a>
  </li>
  <li class="nav-item" data-depth="3">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../../core/expressions/language-ref.html">Language Reference</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../core/expressions/language-ref/literal.html">Literal Expressions</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../core/expressions/language-ref/properties-arrays.html">Properties, Arrays, Lists, Maps, and Indexers</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../core/expressions/language-ref/inline-lists.html">Inline Lists</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../core/expressions/language-ref/inline-maps.html">Inline Maps</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../core/expressions/language-ref/array-construction.html">Array Construction</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../core/expressions/language-ref/methods.html">Methods</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../core/expressions/language-ref/operators.html">Operators</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../core/expressions/language-ref/types.html">Types</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../core/expressions/language-ref/constructors.html">Constructors</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../core/expressions/language-ref/variables.html">Variables</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../core/expressions/language-ref/functions.html">Functions</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../core/expressions/language-ref/varargs.html">Varargs Invocations</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../core/expressions/language-ref/bean-references.html">Bean References</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../core/expressions/language-ref/operator-ternary.html">Ternary Operator (If-Then-Else)</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../core/expressions/language-ref/operator-elvis.html">The Elvis Operator</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../core/expressions/language-ref/operator-safe-navigation.html">Safe Navigation Operator</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../core/expressions/language-ref/collection-selection.html">Collection Selection</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../core/expressions/language-ref/collection-projection.html">Collection Projection</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../core/expressions/language-ref/templating.html">Expression Templating</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../core/expressions/example-classes.html">Classes Used in the Examples</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../../core/aop.html">Aspect Oriented Programming with Spring</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../core/aop/introduction-defn.html">AOP Concepts</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../core/aop/introduction-spring-defn.html">Spring AOP Capabilities and Goals</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../core/aop/introduction-proxies.html">AOP Proxies</a>
  </li>
  <li class="nav-item" data-depth="3">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../../core/aop/ataspectj.html">@AspectJ support</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../core/aop/ataspectj/aspectj-support.html">Enabling @AspectJ Support</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../core/aop/ataspectj/at-aspectj.html">Declaring an Aspect</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../core/aop/ataspectj/pointcuts.html">Declaring a Pointcut</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../core/aop/ataspectj/advice.html">Declaring Advice</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../core/aop/ataspectj/introductions.html">Introductions</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../core/aop/ataspectj/instantiation-models.html">Aspect Instantiation Models</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../core/aop/ataspectj/example.html">An AOP Example</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../core/aop/schema.html">Schema-based AOP Support</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../core/aop/choosing.html">Choosing which AOP Declaration Style to Use</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../core/aop/mixing-styles.html">Mixing Aspect Types</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../core/aop/proxying.html">Proxying Mechanisms</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../core/aop/aspectj-programmatic.html">Programmatic Creation of @AspectJ Proxies</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../core/aop/using-aspectj.html">Using AspectJ with Spring Applications</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../core/aop/resources.html">Further Resources</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../../core/aop-api.html">Spring AOP APIs</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../core/aop-api/pointcuts.html">Pointcut API in Spring</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../core/aop-api/advice.html">Advice API in Spring</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../core/aop-api/advisor.html">The Advisor API in Spring</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../core/aop-api/pfb.html">Using the <code>ProxyFactoryBean</code> to Create AOP Proxies</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../core/aop-api/concise-proxy.html">Concise Proxy Definitions</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../core/aop-api/prog.html">Creating AOP Proxies Programmatically with the <code>ProxyFactory</code></a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../core/aop-api/advised.html">Manipulating Advised Objects</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../core/aop-api/autoproxy.html">Using the "auto-proxy" facility</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../core/aop-api/targetsource.html">Using <code>TargetSource</code> Implementations</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../core/aop-api/extensibility.html">Defining New Advice Types</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../../core/resilience.html">Resilience Features</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../../core/null-safety.html">Null-safety</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../../core/databuffer-codec.html">Data Buffers and Codecs</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../../core/aot.html">Ahead of Time Optimizations</a>
  </li>
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../../core/appendix.html">Appendix</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../core/appendix/xsd-schemas.html">XML Schemas</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../core/appendix/xml-custom.html">XML Schema Authoring</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../core/appendix/application-startup-steps.html">Application Startup Steps</a>
  </li>
</ul>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="1">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../../data-access.html">Data Access</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../../data-access/transaction.html">Transaction Management</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../data-access/transaction/motivation.html">Advantages of the Spring Framework&#8217;s Transaction Support Model</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../data-access/transaction/strategies.html">Understanding the Spring Framework Transaction Abstraction</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../data-access/transaction/tx-resource-synchronization.html">Synchronizing Resources with Transactions</a>
  </li>
  <li class="nav-item" data-depth="3">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../../data-access/transaction/declarative.html">Declarative Transaction Management</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../data-access/transaction/declarative/tx-decl-explained.html">Understanding the Spring Framework&#8217;s Declarative Transaction Implementation</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../data-access/transaction/declarative/first-example.html">Example of Declarative Transaction Implementation</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../data-access/transaction/declarative/rolling-back.html">Rolling Back a Declarative Transaction</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../data-access/transaction/declarative/diff-tx.html">Configuring Different Transactional Semantics for Different Beans</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../data-access/transaction/declarative/txadvice-settings.html">&lt;tx:advice/&gt; Settings</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../data-access/transaction/declarative/annotations.html">Using <code>@Transactional</code></a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../data-access/transaction/declarative/tx-propagation.html">Transaction Propagation</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../data-access/transaction/declarative/applying-more-than-just-tx-advice.html">Advising Transactional Operations</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../data-access/transaction/declarative/aspectj.html">Using <code>@Transactional</code> with AspectJ</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../data-access/transaction/programmatic.html">Programmatic Transaction Management</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../data-access/transaction/tx-decl-vs-prog.html">Choosing Between Programmatic and Declarative Transaction Management</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../data-access/transaction/event.html">Transaction-bound Events</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../data-access/transaction/application-server-integration.html">Application server-specific integration</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../data-access/transaction/solutions-to-common-problems.html">Solutions to Common Problems</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../data-access/transaction/resources.html">Further Resources</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../../data-access/dao.html">DAO Support</a>
  </li>
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../../data-access/jdbc.html">Data Access with JDBC</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../data-access/jdbc/choose-style.html">Choosing an Approach for JDBC Database Access</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../data-access/jdbc/packages.html">Package Hierarchy</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../data-access/jdbc/core.html">Using the JDBC Core Classes to Control Basic JDBC Processing and Error Handling</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../data-access/jdbc/connections.html">Controlling Database Connections</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../data-access/jdbc/advanced.html">JDBC Batch Operations</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../data-access/jdbc/simple.html">Simplifying JDBC Operations with the <code>SimpleJdbc</code> Classes</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../data-access/jdbc/object.html">Modeling JDBC Operations as Java Objects</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../data-access/jdbc/parameter-handling.html">Common Problems with Parameter and Data Value Handling</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../data-access/jdbc/embedded-database-support.html">Embedded Database Support</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../data-access/jdbc/initializing-datasource.html">Initializing a <code>DataSource</code></a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../../data-access/r2dbc.html">Data Access with R2DBC</a>
  </li>
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../../data-access/orm.html">Object Relational Mapping (ORM) Data Access</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../data-access/orm/introduction.html">Introduction to ORM with Spring</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../data-access/orm/general.html">General ORM Integration Considerations</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../data-access/orm/hibernate.html">Hibernate</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../data-access/orm/jpa.html">JPA</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../../data-access/oxm.html">Marshalling XML by Using Object-XML Mappers</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../../data-access/appendix.html">Appendix</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="1">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../../web.html">Web on Servlet Stack</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../../web/webmvc.html">Spring Web MVC</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../../web/webmvc/mvc-servlet.html">DispatcherServlet</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/webmvc/mvc-servlet/context-hierarchy.html">Context Hierarchy</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/webmvc/mvc-servlet/special-bean-types.html">Special Bean Types</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/webmvc/mvc-servlet/config.html">Web MVC Config</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/webmvc/mvc-servlet/container-config.html">Servlet Config</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/webmvc/mvc-servlet/sequence.html">Processing</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/webmvc/mvc-servlet/handlermapping-path.html">Path Matching</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/webmvc/mvc-servlet/handlermapping-interceptor.html">Interception</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/webmvc/mvc-servlet/exceptionhandlers.html">Exceptions</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/webmvc/mvc-servlet/viewresolver.html">View Resolution</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/webmvc/mvc-servlet/localeresolver.html">Locale</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/webmvc/mvc-servlet/multipart.html">Multipart Resolver</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/webmvc/mvc-servlet/logging.html">Logging</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../web/webmvc/filters.html">Filters</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../web/webmvc/message-converters.html">HTTP Message Conversion</a>
  </li>
  <li class="nav-item" data-depth="3">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../../web/webmvc/mvc-controller.html">Annotated Controllers</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/webmvc/mvc-controller/ann.html">Declaration</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/webmvc/mvc-controller/ann-requestmapping.html">Mapping Requests</a>
  </li>
  <li class="nav-item" data-depth="4">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../../web/webmvc/mvc-controller/ann-methods.html">Handler Methods</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../../web/webmvc/mvc-controller/ann-methods/arguments.html">Method Arguments</a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../../web/webmvc/mvc-controller/ann-methods/return-types.html">Return Values</a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../../web/webmvc/mvc-controller/ann-methods/typeconversion.html">Type Conversion</a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../../web/webmvc/mvc-controller/ann-methods/matrix-variables.html">Matrix Variables</a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../../web/webmvc/mvc-controller/ann-methods/requestparam.html"><code>@RequestParam</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../../web/webmvc/mvc-controller/ann-methods/requestheader.html"><code>@RequestHeader</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../../web/webmvc/mvc-controller/ann-methods/cookievalue.html"><code>@CookieValue</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../../web/webmvc/mvc-controller/ann-methods/modelattrib-method-args.html"><code>@ModelAttribute</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../../web/webmvc/mvc-controller/ann-methods/sessionattributes.html"><code>@SessionAttributes</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../../web/webmvc/mvc-controller/ann-methods/sessionattribute.html"><code>@SessionAttribute</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../../web/webmvc/mvc-controller/ann-methods/requestattrib.html"><code>@RequestAttribute</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../../web/webmvc/mvc-controller/ann-methods/redirecting-passing-data.html">Redirect Attributes</a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../../web/webmvc/mvc-controller/ann-methods/flash-attributes.html">Flash Attributes</a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../../web/webmvc/mvc-controller/ann-methods/multipart-forms.html">Multipart</a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../../web/webmvc/mvc-controller/ann-methods/requestbody.html"><code>@RequestBody</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../../web/webmvc/mvc-controller/ann-methods/httpentity.html">HttpEntity</a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../../web/webmvc/mvc-controller/ann-methods/responsebody.html"><code>@ResponseBody</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../../web/webmvc/mvc-controller/ann-methods/responseentity.html">ResponseEntity</a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../../web/webmvc/mvc-controller/ann-methods/jackson.html">Jackson JSON</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/webmvc/mvc-controller/ann-modelattrib-methods.html">Model</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/webmvc/mvc-controller/ann-initbinder.html"><code>@InitBinder</code></a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/webmvc/mvc-controller/ann-validation.html">Validation</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/webmvc/mvc-controller/ann-exceptionhandler.html">Exceptions</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/webmvc/mvc-controller/ann-advice.html">Controller Advice</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../web/webmvc-functional.html">Functional Endpoints</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../web/webmvc/mvc-uri-building.html">URI Links</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../web/webmvc/mvc-ann-async.html">Asynchronous Requests</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../web/webmvc/mvc-range.html">Range Requests</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../web/webmvc/mvc-data-binding.html">Data Binding</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../web/webmvc-cors.html">CORS</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../web/webmvc-versioning.html">API Versioning</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../web/webmvc/mvc-ann-rest-exceptions.html">Error Responses</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../web/webmvc/mvc-security.html">Web Security</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../web/webmvc/mvc-caching.html">HTTP Caching</a>
  </li>
  <li class="nav-item" data-depth="3">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../../web/webmvc-view.html">View Technologies</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/webmvc-view/mvc-thymeleaf.html">Thymeleaf</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/webmvc-view/mvc-freemarker.html">FreeMarker</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/webmvc-view/mvc-groovymarkup.html">Groovy Markup</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/webmvc-view/mvc-script.html">Script Views</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/webmvc-view/mvc-fragments.html">HTML Fragments</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/webmvc-view/mvc-jsp.html">JSP and JSTL</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/webmvc-view/mvc-feeds.html">RSS and Atom</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/webmvc-view/mvc-document.html">PDF and Excel</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/webmvc-view/mvc-jackson.html">Jackson</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/webmvc-view/mvc-xml-marshalling.html">XML Marshalling</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/webmvc-view/mvc-xslt.html">XSLT Views</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="3">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../../web/webmvc/mvc-config.html">MVC Config</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/webmvc/mvc-config/enable.html">Enable MVC Configuration</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/webmvc/mvc-config/customize.html">MVC Config API</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/webmvc/mvc-config/conversion.html">Type Conversion</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/webmvc/mvc-config/validation.html">Validation</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/webmvc/mvc-config/interceptors.html">Interceptors</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/webmvc/mvc-config/content-negotiation.html">Content Types</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/webmvc/mvc-config/message-converters.html">Message Converters</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/webmvc/mvc-config/view-controller.html">View Controllers</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/webmvc/mvc-config/view-resolvers.html">View Resolvers</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/webmvc/mvc-config/static-resources.html">Static Resources</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/webmvc/mvc-config/default-servlet-handler.html">Default Servlet</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/webmvc/mvc-config/path-matching.html">Path Matching</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/webmvc/mvc-config/api-version.html">API Version</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/webmvc/mvc-config/advanced-java.html">Advanced Java Config</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/webmvc/mvc-config/advanced-xml.html">Advanced XML Config</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../web/webmvc/mvc-http2.html">HTTP/2</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../../web/webmvc-client.html">REST Clients</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../../web/webmvc-test.html">Testing</a>
  </li>
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../../web/websocket.html">WebSockets</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../web/websocket/server.html">WebSocket API</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../web/websocket/fallback.html">SockJS Fallback</a>
  </li>
  <li class="nav-item" data-depth="3">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../../web/websocket/stomp.html">STOMP</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/websocket/stomp/overview.html">Overview</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/websocket/stomp/benefits.html">Benefits</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/websocket/stomp/enable.html">Enable STOMP</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/websocket/stomp/server-config.html">WebSocket Transport</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/websocket/stomp/message-flow.html">Flow of Messages</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/websocket/stomp/handle-annotations.html">Annotated Controllers</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/websocket/stomp/handle-send.html">Sending Messages</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/websocket/stomp/handle-simple-broker.html">Simple Broker</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/websocket/stomp/handle-broker-relay.html">External Broker</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/websocket/stomp/handle-broker-relay-configure.html">Connecting to a Broker</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/websocket/stomp/destination-separator.html">Dots as Separators</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/websocket/stomp/authentication.html">Authentication</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/websocket/stomp/authentication-token-based.html">Token Authentication</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/websocket/stomp/authorization.html">Authorization</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/websocket/stomp/user-destination.html">User Destinations</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/websocket/stomp/ordered-messages.html">Order of Messages</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/websocket/stomp/application-context-events.html">Events</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/websocket/stomp/interceptors.html">Interception</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/websocket/stomp/client.html">STOMP Client</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/websocket/stomp/scope.html">WebSocket Scope</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/websocket/stomp/configuration-performance.html">Performance</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/websocket/stomp/stats.html">Monitoring</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/websocket/stomp/testing.html">Testing</a>
  </li>
</ul>
  </li>
</ul>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="1">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../../web-reactive.html">Web on Reactive Stack</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../../web/webflux.html">Spring WebFlux</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../web/webflux/new-framework.html">Overview</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../web/webflux/reactive-spring.html">Reactive Core</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../web/webflux/dispatcher-handler.html"><code>DispatcherHandler</code></a>
  </li>
  <li class="nav-item" data-depth="3">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../../web/webflux/controller.html">Annotated Controllers</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/webflux/controller/ann.html"><code>@Controller</code></a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/webflux/controller/ann-requestmapping.html">Mapping Requests</a>
  </li>
  <li class="nav-item" data-depth="4">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../../web/webflux/controller/ann-methods.html">Handler Methods</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../../web/webflux/controller/ann-methods/arguments.html">Method Arguments</a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../../web/webflux/controller/ann-methods/return-types.html">Return Values</a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../../web/webflux/controller/ann-methods/typeconversion.html">Type Conversion</a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../../web/webflux/controller/ann-methods/matrix-variables.html">Matrix Variables</a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../../web/webflux/controller/ann-methods/requestparam.html"><code>@RequestParam</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../../web/webflux/controller/ann-methods/requestheader.html"><code>@RequestHeader</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../../web/webflux/controller/ann-methods/cookievalue.html"><code>@CookieValue</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../../web/webflux/controller/ann-methods/modelattrib-method-args.html"><code>@ModelAttribute</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../../web/webflux/controller/ann-methods/sessionattributes.html"><code>@SessionAttributes</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../../web/webflux/controller/ann-methods/sessionattribute.html"><code>@SessionAttribute</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../../web/webflux/controller/ann-methods/requestattrib.html"><code>@RequestAttribute</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../../web/webflux/controller/ann-methods/multipart-forms.html">Multipart Content</a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../../web/webflux/controller/ann-methods/requestbody.html"><code>@RequestBody</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../../web/webflux/controller/ann-methods/httpentity.html"><code>HttpEntity</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../../web/webflux/controller/ann-methods/responsebody.html"><code>@ResponseBody</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../../web/webflux/controller/ann-methods/responseentity.html"><code>ResponseEntity</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../../web/webflux/controller/ann-methods/jackson.html">Jackson JSON</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/webflux/controller/ann-modelattrib-methods.html"><code>Model</code></a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/webflux/controller/ann-initbinder.html"><code>DataBinder</code></a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/webflux/controller/ann-validation.html">Validation</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/webflux/controller/ann-exceptions.html">Exceptions</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../web/webflux/controller/ann-advice.html">Controller Advice</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../web/webflux-functional.html">Functional Endpoints</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../web/webflux/uri-building.html">URI Links</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../web/webflux/range.html">Range Requests</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../web/webflux/data-binding.html">Data Binding</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../web/webflux-cors.html">CORS</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../web/webflux-versioning.html">API Versioning</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../web/webflux/ann-rest-exceptions.html">Error Responses</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../web/webflux/security.html">Web Security</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../web/webflux/caching.html">HTTP Caching</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../web/webflux-view.html">View Technologies</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../web/webflux/config.html">WebFlux Config</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../web/webflux/http2.html">HTTP/2</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../../web/webflux-webclient.html">WebClient</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../web/webflux-webclient/client-builder.html">Configuration</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../web/webflux-webclient/client-retrieve.html"><code>retrieve()</code></a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../web/webflux-webclient/client-exchange.html">Exchange</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../web/webflux-webclient/client-body.html">Request Body</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../web/webflux-webclient/client-filter.html">Filters</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../web/webflux-webclient/client-attributes.html">Attributes</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../web/webflux-webclient/client-context.html">Context</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../web/webflux-webclient/client-synchronous.html">Synchronous Use</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../web/webflux-webclient/client-testing.html">Testing</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../../web/webflux-http-service-client.html">HTTP Service Client</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../../web/webflux-websocket.html">WebSockets</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../../web/webflux-test.html">Testing</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../../rsocket.html">RSocket</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../../web/webflux-reactive-libraries.html">Reactive Libraries</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="1">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../../testing.html">Testing</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../../testing/introduction.html">Introduction to Spring Testing</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../../testing/unit.html">Unit Testing</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../../testing/integration.html">Integration Testing</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../../testing/support-jdbc.html">JDBC Testing Support</a>
  </li>
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../../testing/testcontext-framework.html">Spring TestContext Framework</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../testing/testcontext-framework/key-abstractions.html">Key Abstractions</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../testing/testcontext-framework/bootstrapping.html">Bootstrapping the TestContext Framework</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../testing/testcontext-framework/tel-config.html"><code>TestExecutionListener</code> Configuration</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../testing/testcontext-framework/application-events.html">Application Events</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../testing/testcontext-framework/test-execution-events.html">Test Execution Events</a>
  </li>
  <li class="nav-item" data-depth="3">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../../testing/testcontext-framework/ctx-management.html">Context Management</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../testing/testcontext-framework/ctx-management/javaconfig.html">Context Configuration with Component Classes</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../testing/testcontext-framework/ctx-management/xml.html">Context Configuration with XML Resources</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../testing/testcontext-framework/ctx-management/groovy.html">Context Configuration with Groovy Scripts</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../testing/testcontext-framework/ctx-management/default-config.html">Default Context Configuration</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../testing/testcontext-framework/ctx-management/mixed-config.html">Mixing Component Classes, XML, and Groovy Scripts</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../testing/testcontext-framework/ctx-management/context-customizers.html">Context Configuration with Context Customizers</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../testing/testcontext-framework/ctx-management/initializers.html">Context Configuration with Context Initializers</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../testing/testcontext-framework/ctx-management/inheritance.html">Context Configuration Inheritance</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../testing/testcontext-framework/ctx-management/env-profiles.html">Context Configuration with Environment Profiles</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../testing/testcontext-framework/ctx-management/property-sources.html">Context Configuration with Test Property Sources</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../testing/testcontext-framework/ctx-management/dynamic-property-sources.html">Context Configuration with Dynamic Property Sources</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../testing/testcontext-framework/ctx-management/web.html">Loading a <code>WebApplicationContext</code></a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../testing/testcontext-framework/ctx-management/web-mocks.html">Working with Web Mocks</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../testing/testcontext-framework/ctx-management/caching.html">Context Caching</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../testing/testcontext-framework/ctx-management/context-pausing.html">Context Pausing</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../testing/testcontext-framework/ctx-management/failure-threshold.html">Context Failure Threshold</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../testing/testcontext-framework/ctx-management/hierarchies.html">Context Hierarchies</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../testing/testcontext-framework/fixture-di.html">Dependency Injection of Test Fixtures</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../testing/testcontext-framework/bean-overriding.html">Bean Overriding in Tests</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../testing/testcontext-framework/web-scoped-beans.html">Testing Request- and Session-scoped Beans</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../testing/testcontext-framework/tx.html">Transaction Management</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../testing/testcontext-framework/executing-sql.html">Executing SQL Scripts</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../testing/testcontext-framework/parallel-test-execution.html">Parallel Test Execution</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../testing/testcontext-framework/support-classes.html">TestContext Framework Support Classes</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../testing/testcontext-framework/aot.html">Ahead of Time Support for Tests</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../../testing/webtestclient.html">WebTestClient</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../../testing/resttestclient.html">RestTestClient</a>
  </li>
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../../testing/mockmvc.html">MockMvc</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../testing/mockmvc/overview.html">Overview</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../testing/mockmvc/setup-options.html">Setup Options</a>
  </li>
  <li class="nav-item" data-depth="3">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../../testing/mockmvc/hamcrest.html">Hamcrest Integration</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../testing/mockmvc/hamcrest/static-imports.html">Static Imports</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../testing/mockmvc/hamcrest/setup.html">Configuring MockMvc</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../testing/mockmvc/hamcrest/setup-steps.html">Setup Features</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../testing/mockmvc/hamcrest/requests.html">Performing Requests</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../testing/mockmvc/hamcrest/expectations.html">Defining Expectations</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../testing/mockmvc/hamcrest/async-requests.html">Async Requests</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../testing/mockmvc/hamcrest/vs-streaming-response.html">Streaming Responses</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../testing/mockmvc/hamcrest/filters.html">Filter Registrations</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="3">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../../testing/mockmvc/assertj.html">AssertJ Integration</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../testing/mockmvc/assertj/setup.html">Configuring MockMvcTester</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../testing/mockmvc/assertj/requests.html">Performing Requests</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../testing/mockmvc/assertj/assertions.html">Defining Expectations</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../testing/mockmvc/assertj/integration.html">MockMvc integration</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="3">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../../testing/mockmvc/htmlunit.html">HtmlUnit Integration</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../testing/mockmvc/htmlunit/why.html">Why HtmlUnit Integration?</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../testing/mockmvc/htmlunit/mah.html">MockMvc and HtmlUnit</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../testing/mockmvc/htmlunit/webdriver.html">MockMvc and WebDriver</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../testing/mockmvc/htmlunit/geb.html">MockMvc and Geb</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../testing/mockmvc/vs-end-to-end-integration-tests.html">MockMvc vs End-to-End Tests</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../testing/mockmvc/resources.html">Further Examples</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../../testing/spring-mvc-test-client.html">Testing Client Applications</a>
  </li>
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../../testing/appendix.html">Appendix</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../../testing/annotations.html">Annotations</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../testing/annotations/integration-standard.html">Standard Annotation Support</a>
  </li>
  <li class="nav-item" data-depth="4">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../../testing/annotations/integration-spring.html">Spring Testing Annotations</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../../testing/annotations/integration-spring/annotation-bootstrapwith.html"><code>@BootstrapWith</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../../testing/annotations/integration-spring/annotation-contextconfiguration.html"><code>@ContextConfiguration</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../../testing/annotations/integration-spring/annotation-webappconfiguration.html"><code>@WebAppConfiguration</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../../testing/annotations/integration-spring/annotation-contexthierarchy.html"><code>@ContextHierarchy</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../../testing/annotations/integration-spring/annotation-contextcustomizerfactories.html"><code>@ContextCustomizerFactories</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../../testing/annotations/integration-spring/annotation-activeprofiles.html"><code>@ActiveProfiles</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../../testing/annotations/integration-spring/annotation-testpropertysource.html"><code>@TestPropertySource</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../../testing/annotations/integration-spring/annotation-dynamicpropertysource.html"><code>@DynamicPropertySource</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../../testing/annotations/integration-spring/annotation-testbean.html"><code>@TestBean</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../../testing/annotations/integration-spring/annotation-mockitobean.html"><code>@MockitoBean</code> and <code>@MockitoSpyBean</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../../testing/annotations/integration-spring/annotation-dirtiescontext.html"><code>@DirtiesContext</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../../testing/annotations/integration-spring/annotation-testexecutionlisteners.html"><code>@TestExecutionListeners</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../../testing/annotations/integration-spring/annotation-recordapplicationevents.html"><code>@RecordApplicationEvents</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../../testing/annotations/integration-spring/annotation-commit.html"><code>@Commit</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../../testing/annotations/integration-spring/annotation-rollback.html"><code>@Rollback</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../../testing/annotations/integration-spring/annotation-beforetransaction.html"><code>@BeforeTransaction</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../../testing/annotations/integration-spring/annotation-aftertransaction.html"><code>@AfterTransaction</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../../testing/annotations/integration-spring/annotation-sql.html"><code>@Sql</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../../testing/annotations/integration-spring/annotation-sqlconfig.html"><code>@SqlConfig</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../../testing/annotations/integration-spring/annotation-sqlmergemode.html"><code>@SqlMergeMode</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../../testing/annotations/integration-spring/annotation-sqlgroup.html"><code>@SqlGroup</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../../testing/annotations/integration-spring/annotation-disabledinaotmode.html"><code>@DisabledInAotMode</code></a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../testing/annotations/integration-junit4.html">Spring JUnit 4 Testing Annotations</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../testing/annotations/integration-junit-jupiter.html">Spring JUnit Jupiter Testing Annotations</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../../testing/annotations/integration-meta.html">Meta-Annotation Support for Testing</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../testing/resources.html">Further Resources</a>
  </li>
</ul>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="1">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../../integration.html">Integration</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../rest-clients.html">REST Clients</a>
  </li>
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../jms.html">JMS (Java Message Service)</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../jms/using.html">Using Spring JMS</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../jms/sending.html">Sending a Message</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../jms/receiving.html">Receiving a Message</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../jms/jca-message-endpoint-manager.html">Support for JCA Message Endpoints</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../jms/annotated.html">Annotation-driven Listener Endpoints</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../jms/namespace.html">JMS Namespace Support</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../jmx.html">JMX</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../jmx/exporting.html">Exporting Your Beans to JMX</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../jmx/interface.html">Controlling the Management Interface of Your Beans</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../jmx/naming.html">Controlling  <code>ObjectName</code> Instances for Your Beans</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../jmx/jsr160.html">Using JSR-160 Connectors</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../jmx/proxy.html">Accessing MBeans through Proxies</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../jmx/notifications.html">Notifications</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../jmx/resources.html">Further Resources</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../email.html">Email</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../scheduling.html">Task Execution and Scheduling</a>
  </li>
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../cache.html">Cache Abstraction</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="strategies.html">Understanding the Cache Abstraction</a>
  </li>
  <li class="nav-item is-current-page" data-depth="3">
    <a class="nav-link"  href="annotations.html">Declarative Annotation-based Caching</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="jsr-107.html">JCache (JSR-107) Annotations</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="declarative-xml.html">Declarative XML-based Caching</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="store-configuration.html">Configuring the Cache Storage</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="plug.html">Plugging-in Different Back-end Caches</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="specific-config.html">How can I Set the TTL/TTI/Eviction policy/XXX feature?</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../observability.html">Observability Support</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../aot-cache.html">JVM AOT Cache</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../checkpoint-restore.html">JVM Checkpoint Restore</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../appendix.html">Appendix</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="1">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../../languages.html">Language Support</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../../languages/kotlin.html">Kotlin</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../languages/kotlin/requirements.html">Requirements</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../languages/kotlin/extensions.html">Extensions</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../languages/kotlin/null-safety.html">Null-safety</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../languages/kotlin/classes-interfaces.html">Classes and Interfaces</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../languages/kotlin/annotations.html">Annotations</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../languages/kotlin/bean-registration-dsl.html">Bean Registration DSL</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../languages/kotlin/web.html">Web</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../languages/kotlin/coroutines.html">Coroutines</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../languages/kotlin/spring-projects-in.html">Spring Projects in Kotlin</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../languages/kotlin/getting-started.html">Getting Started</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../languages/kotlin/resources.html">Resources</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../../languages/groovy.html">Apache Groovy</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="1">
    <a class="nav-link"  href="../../appendix.html">Appendix</a>
  </li>
  <li class="nav-item" data-depth="1">
    <a class="nav-link link-external"  target="_blank" href="https://docs.spring.io/spring-framework/docs/7.0.9/javadoc-api/">Java API</a>
  </li>
  <li class="nav-item" data-depth="1">
    <a class="nav-link link-external"  target="_blank" href="https://docs.spring.io/spring-framework/docs/7.0.9/kdoc-api/">Kotlin API</a>
  </li>
  <li class="nav-item" data-depth="1">
    <a class="nav-link link-external"  target="_blank" href="https://github.com/spring-projects/spring-framework/wiki">Wiki</a>
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
      data-title="Declarative Annotation-based Caching"
      data-levels="2"
    >
      <div class="toc-menu"></div>
    </div>
    <div class="sidebar-links">
        <a href="https://github.com/spring-projects/spring-framework/blob/v7.0.9/framework-docs/modules/ROOT/pages/integration/cache/annotations.adoc">
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
              <a href="https://github.com/spring-projects/spring-framework" title="GitHub">
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
        <a href="https://stackoverflow.com/questions/tagged/spring">
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
      <li><a href="../../index.html">Spring Framework</a></li>
      <li><a href="../../integration.html">Integration</a></li>
      <li><a href="../cache.html">Cache Abstraction</a></li>
      <li><a href="annotations.html">Declarative Annotation-based Caching</a></li>
    </ul>
  </nav>
</div><h1 id="page-title" class="page">Declarative Annotation-based Caching</h1>
<div id="preamble">
<div class="sectionbody">
<div class="paragraph">
<p>For caching declaration, Spring&#8217;s caching abstraction provides a set of Java annotations:</p>
</div>
<div class="ulist">
<ul>
<li>
<p><code>@Cacheable</code>: Triggers cache population.</p>
</li>
<li>
<p><code>@CacheEvict</code>: Triggers cache eviction.</p>
</li>
<li>
<p><code>@CachePut</code>: Updates the cache without interfering with the method execution.</p>
</li>
<li>
<p><code>@Caching</code>: Regroups multiple cache operations to be applied on a method.</p>
</li>
<li>
<p><code>@CacheConfig</code>: Shares some common cache-related settings at class-level.</p>
</li>
</ul>
</div>
</div>
</div>
<div class="sect1">
<h2 id="cache-annotations-cacheable"><a class="anchor" href="#cache-annotations-cacheable"></a>The <code>@Cacheable</code> Annotation</h2>
<div class="sectionbody">
<div class="paragraph">
<p>As the name implies, you can use <code>@Cacheable</code> to demarcate methods that are cacheable&#8201;&#8212;&#8201;that is, methods for which the result is stored in the cache so that, on subsequent
invocations (with the same arguments), the value in the cache is returned without
having to actually invoke the method. In its simplest form, the annotation declaration
requires the name of the cache associated with the annotated method, as the following
example shows:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Cacheable("books")
public Book findBook(ISBN isbn) {...}</code></pre>
</div>
</div>
<div class="paragraph">
<p>In the preceding snippet, the <code>findBook</code> method is associated with the cache named <code>books</code>.
Each time the method is called, the cache is checked to see whether the invocation has
already been run and does not have to be repeated. While in most cases, only one
cache is declared, the annotation lets multiple names be specified so that more than one
cache is being used. In this case, each of the caches is checked before invoking the
method&#8201;&#8212;&#8201;if at least one cache is hit, the associated value is returned.</p>
</div>
<div class="admonitionblock note">
<table>
<tr>
<td class="icon">
<i class="fa icon-note" title="Note"></i>
</td>
<td class="content">
All the other caches that do not contain the value are also updated, even though
the cached method was not actually invoked.
</td>
</tr>
</table>
</div>
<div class="paragraph">
<p>The following example uses <code>@Cacheable</code> on the <code>findBook</code> method with multiple caches:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Cacheable({"books", "isbns"})
public Book findBook(ISBN isbn) {...}</code></pre>
</div>
</div>
<div class="sect2">
<h3 id="cache-annotations-cacheable-default-key"><a class="anchor" href="#cache-annotations-cacheable-default-key"></a>Default Key Generation</h3>
<div class="paragraph">
<p>Since caches are essentially key-value stores, each invocation of a cached method
needs to be translated into a suitable key for cache access. The caching abstraction
uses a simple <code>KeyGenerator</code> based on the following algorithm:</p>
</div>
<div class="ulist">
<ul>
<li>
<p>If no parameters are given, return <code>SimpleKey.EMPTY</code>.</p>
</li>
<li>
<p>If only one parameter is given, return that instance.</p>
</li>
<li>
<p>If more than one parameter is given, return a <code>SimpleKey</code> that contains all parameters.</p>
</li>
</ul>
</div>
<div class="paragraph">
<p>This approach works well for most use-cases, as long as parameters have natural keys
and implement valid <code>hashCode()</code> and <code>equals()</code> methods. If that is not the case,
you need to change the strategy.</p>
</div>
<div class="paragraph">
<p>To provide a different default key generator, you need to implement the
<code>org.springframework.cache.interceptor.KeyGenerator</code> interface.</p>
</div>
<div class="admonitionblock note">
<table>
<tr>
<td class="icon">
<i class="fa icon-note" title="Note"></i>
</td>
<td class="content">
<div class="paragraph">
<p>The default key generation strategy changed with the release of Spring 4.0. Earlier
versions of Spring used a key generation strategy that, for multiple key parameters,
considered only the <code>hashCode()</code> of parameters and not <code>equals()</code>. This could cause
unexpected key collisions (see <a href="https://github.com/spring-projects/spring-framework/issues/14870">spring-framework#14870</a>
for background). The new <code>SimpleKeyGenerator</code> uses a compound key for such scenarios.</p>
</div>
<div class="paragraph">
<p>If you want to keep using the previous key strategy, you can configure the deprecated
<code>org.springframework.cache.interceptor.DefaultKeyGenerator</code> class or create a custom
hash-based <code>KeyGenerator</code> implementation.</p>
</div>
</td>
</tr>
</table>
</div>
</div>
<div class="sect2">
<h3 id="cache-annotations-cacheable-key"><a class="anchor" href="#cache-annotations-cacheable-key"></a>Custom Key Generation Declaration</h3>
<div class="paragraph">
<p>Since caching is generic, the target methods are quite likely to have various signatures
that cannot be readily mapped on top of the cache structure. This tends to become obvious
when the target method has multiple arguments out of which only some are suitable for
caching (while the rest are used only by the method logic). Consider the following example:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Cacheable("books")
public Book findBook(ISBN isbn, boolean checkWarehouse, boolean includeUsed)</code></pre>
</div>
</div>
<div class="paragraph">
<p>At first glance, while the two <code>boolean</code> arguments influence the way the book is found,
they are no use for the cache. Furthermore, what if only one of the two is important
while the other is not?</p>
</div>
<div class="paragraph">
<p>For such cases, the <code>@Cacheable</code> annotation lets you specify how the key is generated
through its <code>key</code> attribute. You can use <a href="../../core/expressions.html" class="xref page">SpEL</a> to pick the
arguments of interest (or their nested properties), perform operations, or even
invoke arbitrary methods without having to write any code or implement any interface.
This is the recommended approach over the
<a href="#cache-annotations-cacheable-default-key">default generator</a>,
since methods tend to be quite different in signatures as the code base grows. While the
default strategy might work for some methods, it rarely works for all methods.</p>
</div>
<div class="paragraph">
<p>The following examples use various SpEL declarations (if you are not familiar with SpEL,
do yourself a favor and read <a href="../../core/expressions.html" class="xref page">Spring Expression Language</a>):</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Cacheable(cacheNames="books", key="#isbn")
public Book findBook(ISBN isbn, boolean checkWarehouse, boolean includeUsed)

@Cacheable(cacheNames="books", key="#isbn.rawNumber")
public Book findBook(ISBN isbn, boolean checkWarehouse, boolean includeUsed)

@Cacheable(cacheNames="books", key="T(someType).hash(#isbn)")
public Book findBook(ISBN isbn, boolean checkWarehouse, boolean includeUsed)</code></pre>
</div>
</div>
<div class="paragraph">
<p>The preceding snippets show how easy it is to select a certain argument, one of its
properties, or even an arbitrary (static) method.</p>
</div>
<div class="paragraph">
<p>If the algorithm responsible for generating the key is too specific or if it needs
to be shared, you can define a custom <code>keyGenerator</code> on the operation. To do so,
specify the name of the <code>KeyGenerator</code> bean implementation to use, as the following
example shows:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Cacheable(cacheNames="books", keyGenerator="myKeyGenerator")
public Book findBook(ISBN isbn, boolean checkWarehouse, boolean includeUsed)</code></pre>
</div>
</div>
<div class="admonitionblock note">
<table>
<tr>
<td class="icon">
<i class="fa icon-note" title="Note"></i>
</td>
<td class="content">
The <code>key</code> and <code>keyGenerator</code> parameters are mutually exclusive and an operation
that specifies both results in an exception.
</td>
</tr>
</table>
</div>
</div>
<div class="sect2">
<h3 id="cache-annotations-cacheable-default-cache-resolver"><a class="anchor" href="#cache-annotations-cacheable-default-cache-resolver"></a>Default Cache Resolution</h3>
<div class="paragraph">
<p>The caching abstraction uses a simple <code>CacheResolver</code> that retrieves the caches
defined at the operation level by using the configured <code>CacheManager</code>.</p>
</div>
<div class="paragraph">
<p>To provide a different default cache resolver, you need to implement the
<code>org.springframework.cache.interceptor.CacheResolver</code> interface.</p>
</div>
</div>
<div class="sect2">
<h3 id="cache-annotations-cacheable-cache-resolver"><a class="anchor" href="#cache-annotations-cacheable-cache-resolver"></a>Custom Cache Resolution</h3>
<div class="paragraph">
<p>The default cache resolution fits well for applications that work with a
single <code>CacheManager</code> and have no complex cache resolution requirements.</p>
</div>
<div class="paragraph">
<p>For applications that work with several cache managers, you can set the
<code>cacheManager</code> to use for each operation, as the following example shows:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Cacheable(cacheNames="books", cacheManager="anotherCacheManager") <i class="conum" data-value="1"></i><b>(1)</b>
public Book findBook(ISBN isbn) {...}</code></pre>
</div>
</div>
<div class="colist arabic">
<table>
<tr>
<td><i class="conum" data-value="1"></i><b>1</b></td>
<td>Specifying <code>anotherCacheManager</code>.</td>
</tr>
</table>
</div>
<div class="paragraph">
<p>You can also replace the <code>CacheResolver</code> entirely in a fashion similar to that of
replacing <a href="#cache-annotations-cacheable-key">key generation</a>.
The resolution is requested for every cache operation, letting the implementation
actually resolve the caches to use based on runtime arguments. The following example
shows how to specify a <code>CacheResolver</code>:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Cacheable(cacheResolver="runtimeCacheResolver") <i class="conum" data-value="1"></i><b>(1)</b>
public Book findBook(ISBN isbn) {...}</code></pre>
</div>
</div>
<div class="colist arabic">
<table>
<tr>
<td><i class="conum" data-value="1"></i><b>1</b></td>
<td>Specifying the <code>CacheResolver</code>.</td>
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
<p>Since Spring 4.1, the <code>value</code> attribute of the cache annotations are no longer
mandatory, since this particular information can be provided by the <code>CacheResolver</code>
regardless of the content of the annotation.</p>
</div>
<div class="paragraph">
<p>Similarly to <code>key</code> and <code>keyGenerator</code>, the <code>cacheManager</code> and <code>cacheResolver</code>
parameters are mutually exclusive, and an operation specifying both
results in an exception, as a custom <code>CacheManager</code> is ignored by the
<code>CacheResolver</code> implementation. This is probably not what you expect.</p>
</div>
</td>
</tr>
</table>
</div>
</div>
<div class="sect2">
<h3 id="cache-annotations-cacheable-synchronized"><a class="anchor" href="#cache-annotations-cacheable-synchronized"></a>Synchronized Caching</h3>
<div class="paragraph">
<p>In a multi-threaded environment, certain operations might be concurrently invoked for
the same argument (typically on startup). By default, the cache abstraction does not
lock anything, and the same value may be computed several times, defeating the purpose
of caching.</p>
</div>
<div class="paragraph">
<p>For those particular cases, you can use the <code>sync</code> attribute to instruct the underlying
cache provider to lock the cache entry while the value is being computed. As a result,
only one thread is busy computing the value, while the others are blocked until the entry
is updated in the cache. The following example shows how to use the <code>sync</code> attribute:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Cacheable(cacheNames="foos", sync=true) <i class="conum" data-value="1"></i><b>(1)</b>
public Foo executeExpensiveOperation(String id) {...}</code></pre>
</div>
</div>
<div class="colist arabic">
<table>
<tr>
<td><i class="conum" data-value="1"></i><b>1</b></td>
<td>Using the <code>sync</code> attribute.</td>
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
This is an optional feature, and your favorite cache library may not support it.
All <code>CacheManager</code> implementations provided by the core framework support it. See the
documentation of your cache provider for more details.
</td>
</tr>
</table>
</div>
</div>
<div class="sect2">
<h3 id="cache-annotations-cacheable-reactive"><a class="anchor" href="#cache-annotations-cacheable-reactive"></a>Caching with CompletableFuture and Reactive Return Types</h3>
<div class="paragraph">
<p>As of 6.1, cache annotations take <code>CompletableFuture</code> and reactive return types
into account, automatically adapting the cache interaction accordingly.</p>
</div>
<div class="paragraph">
<p>For a method returning a <code>CompletableFuture</code>, the object produced by that future
will be cached whenever it is complete, and the cache lookup for a cache hit will
be retrieved via a <code>CompletableFuture</code>:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Cacheable("books")
public CompletableFuture&lt;Book&gt; findBook(ISBN isbn) {...}</code></pre>
</div>
</div>
<div class="paragraph">
<p>For a method returning a Reactor <code>Mono</code>, the object emitted by that Reactive Streams
publisher will be cached whenever it is available, and the cache lookup for a cache
hit will be retrieved as a <code>Mono</code> (backed by a <code>CompletableFuture</code>):</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Cacheable("books")
public Mono&lt;Book&gt; findBook(ISBN isbn) {...}</code></pre>
</div>
</div>
<div class="paragraph">
<p>For a method returning a Reactor <code>Flux</code>, the objects emitted by that Reactive Streams
publisher will be collected into a <code>List</code> and cached whenever that list is complete,
and the cache lookup for a cache hit will be retrieved as a <code>Flux</code> (backed by a
<code>CompletableFuture</code> for the cached <code>List</code> value):</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Cacheable("books")
public Flux&lt;Book&gt; findBooks(String author) {...}</code></pre>
</div>
</div>
<div class="paragraph">
<p>Such <code>CompletableFuture</code> and reactive adaptation also works for synchronized caching,
computing the value only once in case of a concurrent cache miss:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Cacheable(cacheNames="foos", sync=true) <i class="conum" data-value="1"></i><b>(1)</b>
public CompletableFuture&lt;Foo&gt; executeExpensiveOperation(String id) {...}</code></pre>
</div>
</div>
<div class="colist arabic">
<table>
<tr>
<td><i class="conum" data-value="1"></i><b>1</b></td>
<td>Using the <code>sync</code> attribute.</td>
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
In order for such an arrangement to work at runtime, the configured cache
needs to be capable of <code>CompletableFuture</code>-based retrieval. The Spring-provided
<code>ConcurrentMapCacheManager</code> automatically adapts to that retrieval style, and
<code>CaffeineCacheManager</code> natively supports it when its asynchronous cache mode is
enabled: set <code>setAsyncCacheMode(true)</code> on your <code>CaffeineCacheManager</code> instance.
</td>
</tr>
</table>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Bean
CacheManager cacheManager() {
	CaffeineCacheManager cacheManager = new CaffeineCacheManager();
	cacheManager.setCacheSpecification(...);
	cacheManager.setAsyncCacheMode(true);
	return cacheManager;
}</code></pre>
</div>
</div>
<div class="paragraph">
<p>Last but not least, be aware that annotation-driven caching is not appropriate
for sophisticated reactive interactions involving composition and back pressure.
If you choose to declare <code>@Cacheable</code> on specific reactive methods, consider the
impact of the rather coarse-granular cache interaction which simply stores the
emitted object for a <code>Mono</code> or even a pre-collected list of objects for a <code>Flux</code>.</p>
</div>
</div>
<div class="sect2">
<h3 id="cache-annotations-cacheable-condition"><a class="anchor" href="#cache-annotations-cacheable-condition"></a>Conditional Caching</h3>
<div class="paragraph">
<p>Sometimes, a method might not be suitable for caching all the time (for example, it might
depend on the given arguments). The cache annotations support such use cases through the
<code>condition</code> parameter, which takes a <code>SpEL</code> expression that is evaluated to either <code>true</code>
or <code>false</code>. If <code>true</code>, the method is cached. If not, it behaves as if the method is not
cached (that is, the method is invoked every time no matter what values are in the cache
or what arguments are used). For example, the following method is cached only if the
argument <code>name</code> has a length shorter than 32:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Cacheable(cacheNames="book", condition="#name.length() &lt; 32") <i class="conum" data-value="1"></i><b>(1)</b>
public Book findBook(String name)</code></pre>
</div>
</div>
<div class="colist arabic">
<table>
<tr>
<td><i class="conum" data-value="1"></i><b>1</b></td>
<td>Setting a condition on <code>@Cacheable</code>.</td>
</tr>
</table>
</div>
<div class="paragraph">
<p>In addition to the <code>condition</code> parameter, you can use the <code>unless</code> parameter to veto the
adding of a value to the cache. Unlike <code>condition</code>, <code>unless</code> expressions are evaluated
after the method has been invoked. To expand on the previous example, perhaps we only
want to cache paperback books, as the following example does:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Cacheable(cacheNames="book", condition="#name.length() &lt; 32", unless="#result.hardback") <i class="conum" data-value="1"></i><b>(1)</b>
public Book findBook(String name)</code></pre>
</div>
</div>
<div class="colist arabic">
<table>
<tr>
<td><i class="conum" data-value="1"></i><b>1</b></td>
<td>Using the <code>unless</code> attribute to block hardbacks.</td>
</tr>
</table>
</div>
<div class="paragraph">
<p>The cache abstraction supports <code>java.util.Optional</code> return types. If an <code>Optional</code> value
is <em>present</em>, it will be stored in the associated cache. If an <code>Optional</code> value is not
present, <code>null</code> will be stored in the associated cache. <code>#result</code> always refers to the
business entity and never a supported wrapper, so the previous example can be rewritten
as follows:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Cacheable(cacheNames="book", condition="#name.length() &lt; 32", unless="#result?.hardback")
public Optional&lt;Book&gt; findBook(String name)</code></pre>
</div>
</div>
<div class="paragraph">
<p>Note that <code>#result</code> still refers to <code>Book</code> and not <code>Optional&lt;Book&gt;</code>. Since it might be
<code>null</code>, we use SpEL&#8217;s <a href="../../core/expressions/language-ref/operator-safe-navigation.html" class="xref page">safe navigation operator</a>.</p>
</div>
</div>
<div class="sect2">
<h3 id="cache-spel-context"><a class="anchor" href="#cache-spel-context"></a>Available Caching SpEL Evaluation Context</h3>
<div class="paragraph">
<p>Each <code>SpEL</code> expression evaluates against a dedicated <a href="../../core/expressions/language-ref.html" class="xref page"><code>context</code></a>.
In addition to the built-in parameters, the framework provides dedicated caching-related
metadata, such as the argument names. The following table describes the items made
available to the context so that you can use them for key and conditional computations:</p>
</div>
<table id="cache-spel-context-tbl" class="tableblock frame-all grid-all stripes-odd stretch">
<caption class="title">Table 1. Cache metadata available in SpEL expressions</caption>
<colgroup>
<col style="width: 25%;">
<col style="width: 25%;">
<col style="width: 25%;">
<col style="width: 25%;">
</colgroup>
<thead>
<tr>
<th class="tableblock halign-left valign-top">Name</th>
<th class="tableblock halign-left valign-top">Location</th>
<th class="tableblock halign-left valign-top">Description</th>
<th class="tableblock halign-left valign-top">Example</th>
</tr>
</thead>
<tbody>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>methodName</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock">Root object</p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock">The name of the method being invoked</p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>#root.methodName</code></p></td>
</tr>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>method</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock">Root object</p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock">The method being invoked</p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>#root.method.name</code></p></td>
</tr>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>target</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock">Root object</p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock">The target object being invoked</p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>#root.target</code></p></td>
</tr>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>targetClass</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock">Root object</p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock">The class of the target being invoked</p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>#root.targetClass</code></p></td>
</tr>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>args</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock">Root object</p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock">The arguments (as an object array) used for invoking the target</p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>#root.args[0]</code></p></td>
</tr>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>caches</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock">Root object</p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock">Collection of caches against which the current method is run</p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>#root.caches[0].name</code></p></td>
</tr>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock">Argument name</p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock">Evaluation context</p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock">The name of a particular method argument. If the names are not available
  (for example, because the code was compiled without the <code>-parameters</code> flag), individual
  arguments are also available using the <code>#a&lt;#arg&gt;</code> syntax where <code>&lt;#arg&gt;</code> stands for the
  argument index (starting from 0).</p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>#iban</code> or <code>#a0</code> (you can also use <code>#p0</code> or <code>#p&lt;#arg&gt;</code> notation as an alias).</p></td>
</tr>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>result</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock">Evaluation context</p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock">The result of the method call (the value to be cached). Only available in <code>unless</code>
  expressions, <code>cache put</code> expressions (to compute the <code>key</code>), or <code>cache evict</code>
  expressions (when <code>beforeInvocation</code> is <code>false</code>). For supported wrappers (such as
  <code>Optional</code>), <code>#result</code> refers to the actual object, not the wrapper.</p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>#result</code></p></td>
</tr>
</tbody>
</table>
</div>
</div>
</div>
<div class="sect1">
<h2 id="cache-annotations-put"><a class="anchor" href="#cache-annotations-put"></a>The <code>@CachePut</code> Annotation</h2>
<div class="sectionbody">
<div class="paragraph">
<p>When the cache needs to be updated without interfering with the method execution,
you can use the <code>@CachePut</code> annotation. That is, the method is always invoked and its
result is placed into the cache (according to the <code>@CachePut</code> options). It supports
the same options as <code>@Cacheable</code> and should be used for cache population rather than
method flow optimization. The following example uses the <code>@CachePut</code> annotation:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@CachePut(cacheNames="book", key="#isbn")
public Book updateBook(ISBN isbn, BookDescriptor descriptor)</code></pre>
</div>
</div>
<div class="admonitionblock important">
<table>
<tr>
<td class="icon">
<i class="fa icon-important" title="Important"></i>
</td>
<td class="content">
Using <code>@CachePut</code> and <code>@Cacheable</code> annotations on the same method is generally
strongly discouraged because they have different behaviors. While the latter causes the
method invocation to be skipped by using the cache, the former forces the invocation in
order to run a cache update. This leads to unexpected behavior and, with the exception
of specific corner-cases (such as annotations having conditions that exclude them from each
other), such declarations should be avoided. Note also that such conditions should not rely
on the result object (that is, the <code>#result</code> variable), as these are validated up-front to
confirm the exclusion.
</td>
</tr>
</table>
</div>
<div class="paragraph">
<p>As of 6.1, <code>@CachePut</code> takes <code>CompletableFuture</code> and reactive return types into account,
performing the put operation whenever the produced object is available.</p>
</div>
</div>
</div>
<div class="sect1">
<h2 id="cache-annotations-evict"><a class="anchor" href="#cache-annotations-evict"></a>The <code>@CacheEvict</code> Annotation</h2>
<div class="sectionbody">
<div class="paragraph">
<p>The cache abstraction allows not just population of a cache store but also eviction.
This process is useful for removing stale or unused data from the cache. As opposed to
<code>@Cacheable</code>, <code>@CacheEvict</code> demarcates methods that perform cache
eviction (that is, methods that act as triggers for removing data from the cache).
Similarly to its sibling, <code>@CacheEvict</code> requires specifying one or more caches
that are affected by the action, allows a custom cache and key resolution or a
condition to be specified, and features an extra parameter
(<code>allEntries</code>) that indicates whether a cache-wide eviction needs to be performed
rather than just an entry eviction (based on the key). The following example evicts
all entries from the <code>books</code> cache:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@CacheEvict(cacheNames="books", allEntries=true) <i class="conum" data-value="1"></i><b>(1)</b>
public void loadBooks(InputStream batch)</code></pre>
</div>
</div>
<div class="colist arabic">
<table>
<tr>
<td><i class="conum" data-value="1"></i><b>1</b></td>
<td>Using the <code>allEntries</code> attribute to evict all entries from the cache.</td>
</tr>
</table>
</div>
<div class="paragraph">
<p>This option comes in handy when an entire cache region needs to be cleared out.
Rather than evicting each entry (which would take a long time, since it is inefficient),
all the entries are removed in one operation, as the preceding example shows.
Note that the framework ignores any key specified in this scenario as it does not apply
(the entire cache is evicted, not only one entry).</p>
</div>
<div class="paragraph">
<p>You can also indicate whether the eviction should occur after (the default) or before
the method is invoked by using the <code>beforeInvocation</code> attribute. The former provides the
same semantics as the rest of the annotations: Once the method completes successfully,
an action (in this case, eviction) on the cache is run. If the method does not
run (as it might be cached) or an exception is thrown, the eviction does not occur.
The latter (<code>beforeInvocation=true</code>) causes the eviction to always occur before the
method is invoked. This is useful in cases where the eviction does not need to be tied
to the method outcome.</p>
</div>
<div class="paragraph">
<p>Note that <code>void</code> methods can be used with <code>@CacheEvict</code> - as the methods act as a
trigger, the return values are ignored (as they do not interact with the cache). This is
not the case with <code>@Cacheable</code> which adds data to the cache or updates data in the cache
and, thus, requires a result.</p>
</div>
<div class="paragraph">
<p>As of 6.1, <code>@CacheEvict</code> takes <code>CompletableFuture</code> and reactive return types into account,
performing an after-invocation evict operation whenever processing has completed.</p>
</div>
</div>
</div>
<div class="sect1">
<h2 id="cache-annotations-caching"><a class="anchor" href="#cache-annotations-caching"></a>The <code>@Caching</code> Annotation</h2>
<div class="sectionbody">
<div class="paragraph">
<p>Sometimes, multiple annotations of the same type (such as <code>@CacheEvict</code> or
<code>@CachePut</code>) need to be specified&#8201;&#8212;&#8201;for example, because the condition or the key
expression is different between different caches. <code>@Caching</code> lets multiple nested
<code>@Cacheable</code>, <code>@CachePut</code>, and <code>@CacheEvict</code> annotations be used on the same method.
The following example uses two <code>@CacheEvict</code> annotations:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Caching(evict = { @CacheEvict("primary"), @CacheEvict(cacheNames="secondary", key="#p0") })
public Book importBooks(String deposit, Date date)</code></pre>
</div>
</div>
</div>
</div>
<div class="sect1">
<h2 id="cache-annotations-config"><a class="anchor" href="#cache-annotations-config"></a>The <code>@CacheConfig</code> Annotation</h2>
<div class="sectionbody">
<div class="paragraph">
<p>So far, we have seen that caching operations offer many customization options and that
you can set these options for each operation. However, some of the customization options
can be tedious to configure if they apply to all operations of the class. For
instance, specifying the name of the cache to use for every cache operation of the
class can be replaced by a single class-level definition. This is where <code>@CacheConfig</code>
comes into play. The following examples uses <code>@CacheConfig</code> to set the name of the cache:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@CacheConfig("books") <i class="conum" data-value="1"></i><b>(1)</b>
public class BookRepositoryImpl implements BookRepository {

	@Cacheable
	public Book findBook(ISBN isbn) {...}
}</code></pre>
</div>
</div>
<div class="colist arabic">
<table>
<tr>
<td><i class="conum" data-value="1"></i><b>1</b></td>
<td>Using <code>@CacheConfig</code> to set the name of the cache.</td>
</tr>
</table>
</div>
<div class="paragraph">
<p><code>@CacheConfig</code> is a class-level annotation that allows sharing the cache names,
the custom <code>KeyGenerator</code>, the custom <code>CacheManager</code>, and the custom <code>CacheResolver</code>.
Placing this annotation on the class does not turn on any caching operation.</p>
</div>
<div class="paragraph">
<p>An operation-level customization always overrides a customization set on <code>@CacheConfig</code>.
Therefore, this gives three levels of customizations for each cache operation:</p>
</div>
<div class="ulist">
<ul>
<li>
<p>Globally configured, for example, through <code>CachingConfigurer</code>: see next section.</p>
</li>
<li>
<p>At the class level, using <code>@CacheConfig</code>.</p>
</li>
<li>
<p>At the operation level.</p>
</li>
</ul>
</div>
<div class="admonitionblock note">
<table>
<tr>
<td class="icon">
<i class="fa icon-note" title="Note"></i>
</td>
<td class="content">
Provider-specific settings are typically available on the <code>CacheManager</code> bean,
for example, on <code>CaffeineCacheManager</code>. These are effectively also global.
</td>
</tr>
</table>
</div>
</div>
</div>
<div class="sect1">
<h2 id="cache-annotation-enable"><a class="anchor" href="#cache-annotation-enable"></a>Enabling Caching Annotations</h2>
<div class="sectionbody">
<div class="paragraph">
<p>It is important to note that even though declaring the cache annotations does not
automatically trigger their actions - like many things in Spring, the feature has to be
declaratively enabled (which means if you ever suspect caching is to blame, you can
disable it by removing only one configuration line rather than all the annotations in
your code).</p>
</div>
<div class="paragraph">
<p>To enable caching annotations add the annotation <code>@EnableCaching</code> to one of your
<code>@Configuration</code> classes or use the <code>cache:annotation-driven</code> element with XML:</p>
</div>
<div id="_tabs_1" class="openblock tabs is-sync is-loading">
<div class="content">
<div class="ulist tablist">
<ul>
<li id="_tabs_1_java" class="tab">
<p>Java</p>
</li>
<li id="_tabs_1_kotlin" class="tab">
<p>Kotlin</p>
</li>
<li id="_tabs_1_xml" class="tab">
<p>Xml</p>
</li>
</ul>
</div>
<div id="_tabs_1_java--panel" class="tabpanel" aria-labelledby="_tabs_1_java">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Configuration
@EnableCaching
class CacheConfiguration {

	@Bean
	CacheManager cacheManager() {
		CaffeineCacheManager cacheManager = new CaffeineCacheManager();
		cacheManager.setCacheSpecification("...");
		return cacheManager;
	}
}</code></pre>
</div>
</div>
</div>
<div id="_tabs_1_kotlin--panel" class="tabpanel" aria-labelledby="_tabs_1_kotlin">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-kotlin hljs" data-lang="kotlin">@Configuration
@EnableCaching
class CacheConfiguration {

	@Bean
	fun cacheManager(): CacheManager {
		return CaffeineCacheManager().apply {
			setCacheSpecification("...")
		}
	}
}</code></pre>
</div>
</div>
</div>
<div id="_tabs_1_xml--panel" class="tabpanel" aria-labelledby="_tabs_1_xml">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-xml hljs" data-lang="xml">&lt;beans xmlns="http://www.springframework.org/schema/beans"
	   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	   xmlns:cache="http://www.springframework.org/schema/cache"
	   xsi:schemaLocation="
			http://www.springframework.org/schema/beans https://www.springframework.org/schema/beans/spring-beans.xsd
			http://www.springframework.org/schema/cache https://www.springframework.org/schema/cache/spring-cache.xsd"&gt;

	&lt;cache:annotation-driven/&gt;

	&lt;bean id="cacheManager" class="org.springframework.cache.caffeine.CaffeineCacheManager"&gt;
		&lt;property name="cacheSpecification" value="..."/&gt;
	&lt;/bean&gt;
&lt;/beans&gt;</code></pre>
</div>
</div>
</div>
</div>
</div>
<div class="paragraph">
<p>Both the <code>cache:annotation-driven</code> element and the <code>@EnableCaching</code> annotation let you
specify various options that influence the way the caching behavior is added to the
application through AOP. The configuration is intentionally similar with that of
<a href="../../data-access/transaction/declarative/annotations.html#tx-annotation-driven-settings" class="xref page"><code>@Transactional</code></a>.</p>
</div>
<div class="admonitionblock note">
<table>
<tr>
<td class="icon">
<i class="fa icon-note" title="Note"></i>
</td>
<td class="content">
The default advice mode for processing caching annotations is <code>proxy</code>, which allows
for interception of calls through the proxy only. Local calls within the same class
cannot get intercepted that way. For a more advanced mode of interception, consider
switching to <code>aspectj</code> mode in combination with compile-time or load-time weaving.
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
For more detail about advanced customizations (using Java configuration) that are
required to implement <code>CachingConfigurer</code>, see the
<a href="https://docs.spring.io/spring-framework/docs/7.0.9/javadoc-api/org/springframework/cache/annotation/CachingConfigurer.html">javadoc</a>.
</td>
</tr>
</table>
</div>
<table id="cache-annotation-driven-settings" class="tableblock frame-all grid-all stripes-odd stretch">
<caption class="title">Table 2. Cache annotation settings</caption>
<colgroup>
<col style="width: 16.6666%;">
<col style="width: 16.6666%;">
<col style="width: 16.6666%;">
<col style="width: 50.0002%;">
</colgroup>
<thead>
<tr>
<th class="tableblock halign-left valign-top">XML Attribute</th>
<th class="tableblock halign-left valign-top">Annotation Attribute</th>
<th class="tableblock halign-left valign-top">Default</th>
<th class="tableblock halign-left valign-top">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>cache-manager</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock">N/A (see the <a href="https://docs.spring.io/spring-framework/docs/7.0.9/javadoc-api/org/springframework/cache/annotation/CachingConfigurer.html"><code>CachingConfigurer</code></a> javadoc)</p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>cacheManager</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock">The name of the cache manager to use. A default <code>CacheResolver</code> is initialized behind
  the scenes with this cache manager (or <code>cacheManager</code> if not set). For more
  fine-grained management of the cache resolution, consider setting the 'cache-resolver'
  attribute.</p></td>
</tr>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>cache-resolver</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock">N/A (see the <a href="https://docs.spring.io/spring-framework/docs/7.0.9/javadoc-api/org/springframework/cache/annotation/CachingConfigurer.html"><code>CachingConfigurer</code></a> javadoc)</p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock">A <code>SimpleCacheResolver</code> using the configured <code>cacheManager</code>.</p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock">The bean name of the CacheResolver that is to be used to resolve the backing caches.
  This attribute is not required and needs to be specified only as an alternative to
  the 'cache-manager' attribute.</p></td>
</tr>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>key-generator</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock">N/A (see the <a href="https://docs.spring.io/spring-framework/docs/7.0.9/javadoc-api/org/springframework/cache/annotation/CachingConfigurer.html"><code>CachingConfigurer</code></a> javadoc)</p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>SimpleKeyGenerator</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock">Name of the custom key generator to use.</p></td>
</tr>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>error-handler</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock">N/A (see the <a href="https://docs.spring.io/spring-framework/docs/7.0.9/javadoc-api/org/springframework/cache/annotation/CachingConfigurer.html"><code>CachingConfigurer</code></a> javadoc)</p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>SimpleCacheErrorHandler</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock">The name of the custom cache error handler to use. By default, any exception thrown during
  a cache related operation is thrown back at the client.</p></td>
</tr>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>mode</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>mode</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>proxy</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock">The default mode (<code>proxy</code>) processes annotated beans to be proxied by using Spring&#8217;s AOP
  framework (following proxy semantics, as discussed earlier, applying to method calls
  coming in through the proxy only). The alternative mode (<code>aspectj</code>) instead weaves the
  affected classes with Spring&#8217;s AspectJ caching aspect, modifying the target class byte
  code to apply to any kind of method call. AspectJ weaving requires <code>spring-aspects.jar</code>
  in the classpath as well as load-time weaving (or compile-time weaving) enabled. (See
  <a href="../../core/aop/using-aspectj.html#aop-aj-ltw-spring" class="xref page">Spring configuration</a> for details on how to set up
  load-time weaving.)</p></td>
</tr>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>proxy-target-class</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>proxyTargetClass</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>false</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock">Applies to proxy mode only. Controls what type of caching proxies are created for
  classes annotated with the <code>@Cacheable</code> or <code>@CacheEvict</code> annotations. If the
  <code>proxy-target-class</code> attribute is set to <code>true</code>, class-based proxies are created.
  If <code>proxy-target-class</code> is <code>false</code> or if the attribute is omitted, standard JDK
  interface-based proxies are created. (See <a href="../../core/aop/proxying.html" class="xref page">Proxying Mechanisms</a>
  for a detailed examination of the different proxy types.)</p></td>
</tr>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>order</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>order</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock">Ordered.LOWEST_PRECEDENCE</p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock">Defines the order of the cache advice that is applied to beans annotated with
  <code>@Cacheable</code> or <code>@CacheEvict</code>. (For more information about the rules related to
  ordering AOP advice, see <a href="../../core/aop/ataspectj/advice.html#aop-ataspectj-advice-ordering" class="xref page">Advice Ordering</a>.)
  No specified ordering means that the AOP subsystem determines the order of the advice.</p></td>
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
<code>&lt;cache:annotation-driven/&gt;</code> looks for <code>@Cacheable/@CachePut/@CacheEvict/@Caching</code>
only on beans in the same application context in which it is defined. This means that,
if you put <code>&lt;cache:annotation-driven/&gt;</code> in a <code>WebApplicationContext</code> for a
<code>DispatcherServlet</code>, it checks for beans only in your controllers, not your services.
See <a href="../../web/webmvc/mvc-servlet.html" class="xref page">the MVC section</a> for more information.
</td>
</tr>
</table>
</div>
<div class="sidebarblock">
<div class="content">
<div class="title">Method visibility and cache annotations</div>
<div class="paragraph">
<p>When you use proxies, you should apply the cache annotations only to methods with
public visibility. If you do annotate protected, private, or package-visible methods
with these annotations, no error is raised, but the annotated method does not exhibit
the configured caching settings. Consider using AspectJ (see the rest of this section)
if you need to annotate non-public methods, as it changes the bytecode itself.</p>
</div>
</div>
</div>
<div class="admonitionblock tip">
<table>
<tr>
<td class="icon">
<i class="fa icon-tip" title="Tip"></i>
</td>
<td class="content">
Spring recommends that you only annotate concrete classes (and methods of concrete
classes) with the <code>@Cache*</code> annotations, as opposed to annotating interfaces.
You certainly can place an <code>@Cache*</code> annotation on an interface (or an interface
method), but this works only if you use the proxy mode (<code>mode="proxy"</code>). If you use the
weaving-based aspect (<code>mode="aspectj"</code>), the caching settings are not recognized on
interface-level declarations by the weaving infrastructure.
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
In proxy mode (the default), only external method calls coming in through the
proxy are intercepted. This means that self-invocation (in effect, a method within the
target object that calls another method of the target object) does not lead to actual
caching at runtime even if the invoked method is marked with <code>@Cacheable</code>. Consider
using the <code>aspectj</code> mode in this case. Also, the proxy must be fully initialized to
provide the expected behavior, so you should not rely on this feature in your
initialization code (that is, <code>@PostConstruct</code>).
</td>
</tr>
</table>
</div>
</div>
</div>
<div class="sect1">
<h2 id="cache-annotation-stereotype"><a class="anchor" href="#cache-annotation-stereotype"></a>Using Custom Annotations</h2>
<div class="sectionbody">
<div class="sidebarblock">
<div class="content">
<div class="title">Custom annotation and AspectJ</div>
<div class="paragraph">
<p>This feature works only with the proxy-based approach but can be enabled
with a bit of extra effort by using AspectJ.</p>
</div>
<div class="paragraph">
<p>The <code>spring-aspects</code> module defines an aspect for the standard annotations only.
If you have defined your own annotations, you also need to define an aspect for
those. Check <code>AnnotationCacheAspect</code> for an example.</p>
</div>
</div>
</div>
<div class="paragraph">
<p>The caching abstraction lets you use your own annotations to identify what method
triggers cache population or eviction. This is quite handy as a template mechanism,
as it eliminates the need to duplicate cache annotation declarations, which is
especially useful if the key or condition are specified or if the foreign imports
(<code>org.springframework</code>) are not allowed in your code base. Similarly to the rest
of the <a href="../../core/beans/classpath-scanning.html#beans-stereotype-annotations" class="xref page">stereotype</a>
annotations, you can use <code>@Cacheable</code>, <code>@CachePut</code>, <code>@CacheEvict</code>, and <code>@CacheConfig</code>
as <a href="../../core/beans/classpath-scanning.html#beans-meta-annotations" class="xref page">meta-annotations</a>
(that is, annotations that can annotate other annotations). In the following example,
we replace a common <code>@Cacheable</code> declaration with our own custom annotation:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Cacheable(cacheNames="books", key="#isbn")
public @interface SlowService {
}</code></pre>
</div>
</div>
<div class="paragraph">
<p>In the preceding example, we have defined our own <code>SlowService</code> annotation,
which itself is annotated with <code>@Cacheable</code>. Now we can replace the following code:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Cacheable(cacheNames="books", key="#isbn")
public Book findBook(ISBN isbn, boolean checkWarehouse, boolean includeUsed)</code></pre>
</div>
</div>
<div class="paragraph">
<p>The following example shows the custom annotation with which we can replace the
preceding code:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@SlowService
public Book findBook(ISBN isbn, boolean checkWarehouse, boolean includeUsed)</code></pre>
</div>
</div>
<div class="paragraph">
<p>Even though <code>@SlowService</code> is not a Spring annotation, the container automatically picks
up its declaration at runtime and understands its meaning. Note that, as mentioned
<a href="#cache-annotation-enable">earlier</a>,
annotation-driven behavior needs to be enabled.</p>
</div>
</div>
</div>
<nav class="pagination">
  <span class="prev"><a href="strategies.html">Understanding the Cache Abstraction</a></span>
  <span class="next"><a href="jsr-107.html">JCache (JSR-107) Annotations</a></span>
</nav>
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
                          <a class="title" href="../../index.html">Spring Framework</a>
                        </div>                        <div class="version-item is-active">
                          <div>
                            <button class="version-toggle" type="button">
                              <span></span>
                              Stable
                            </button>
                          </div>
                          <ul class="versions">
  <li class="version">
    <a href="annotations.html">
      7.0.9
    </a>
  </li>
  <li class="version">
    <a href="../../6.2/integration/cache/annotations.html">
      6.2.19
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
    <a href="../../7.1/integration/cache/annotations.html">
      7.1.0-M1
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
    <a href="../../7.1-SNAPSHOT/integration/cache/annotations.html">
      7.1.0-SNAPSHOT
    </a>
  </li>
  <li class="version">
    <a href="../../7.0-SNAPSHOT/integration/cache/annotations.html">
      7.0.10-SNAPSHOT
    </a>
  </li>
  <li class="version">
    <a href="../../6.2-SNAPSHOT/integration/cache/annotations.html">
      6.2.20-SNAPSHOT
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
<a href="https://docs.spring.io/spring-boot/">
  Spring Boot
</a>
</li>
        <li>
<a href="https://docs.spring.io/spring-framework/reference/">
  Spring Framework
</a>
</li>
        <li>
<a class="anchor"><i class="fa fa-angle-right" aria-hidden="true"></i></a>
  Spring Cloud
<ul>
    <li>
<a href="https://docs.spring.io/spring-cloud-build/reference/">
  Spring Cloud Build
</a>
</li>
    <li>
<a href="https://docs.spring.io/spring-cloud-bus/reference/">
  Spring Cloud Bus
</a>
</li>
    <li>
<a href="https://docs.spring.io/spring-cloud-circuitbreaker/reference/">
  Spring Cloud Circuit Breaker
</a>
</li>
    <li>
<a href="https://docs.spring.io/spring-cloud-commons/reference/">
  Spring Cloud Commons
</a>
</li>
    <li>
<a href="https://docs.spring.io/spring-cloud-config/reference/">
  Spring Cloud Config
</a>
</li>
    <li>
<a href="https://docs.spring.io/spring-cloud-consul/reference/">
  Spring Cloud Consul
</a>
</li>
    <li>
<a href="https://docs.spring.io/spring-cloud-contract/reference/">
  Spring Cloud Contract
</a>
</li>
    <li>
<a href="https://docs.spring.io/spring-cloud-function/reference/">
  Spring Cloud Function
</a>
</li>
    <li>
<a href="https://docs.spring.io/spring-cloud-gateway/reference/">
  Spring Cloud Gateway
</a>
</li>
    <li>
<a href="https://docs.spring.io/spring-cloud-kubernetes/reference/">
  Spring Cloud Kubernetes
</a>
</li>
    <li>
<a href="https://docs.spring.io/spring-cloud-netflix/reference/">
  Spring Cloud Netflix
</a>
</li>
    <li>
<a href="https://docs.spring.io/spring-cloud-openfeign/reference/">
  Spring Cloud OpenFeign
</a>
</li>
    <li>
<a href="https://docs.spring.io/spring-cloud-stream/reference/">
  Spring Cloud Stream
</a>
</li>
    <li>
<a href="https://docs.spring.io/spring-cloud-task/reference/">
  Spring Cloud Task
</a>
</li>
    <li>
<a href="https://docs.spring.io/spring-cloud-vault/reference/">
  Spring Cloud Vault
</a>
</li>
    <li>
<a href="https://docs.spring.io/spring-cloud-zookeeper/reference/">
  Spring Cloud Zookeeper
</a>
</li>
</ul>
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
<a href="https://docs.spring.io/spring-integration/reference/">
  Spring Integration
</a>
</li>
        <li>
<a href="https://docs.spring.io/spring-batch/reference/">
  Spring Batch
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
<a href="https://docs.spring.io/spring-ai/reference/">
  Spring AI
</a>
</li>
        <li>
<a href="https://docs.spring.io/spring-amqp/reference/">
  Spring AMQP
</a>
</li>
        <li>
<a href="https://docs.spring.io/spring-cli/reference/">
  Spring CLI
</a>
</li>
        <li>
<a href="https://docs.spring.io/spring-graphql/reference/">
  Spring GraphQL
</a>
</li>
        <li>
<a href="https://docs.spring.io/spring-kafka/reference/">
  Spring for Apache Kafka
</a>
</li>
        <li>
<a href="https://docs.spring.io/spring-modulith/reference/">
  Spring Modulith
</a>
</li>
        <li>
<a href="https://docs.spring.io/spring-pulsar/reference/">
  Spring for Apache Pulsar
</a>
</li>
        <li>
<a href="https://docs.spring.io/spring-shell/reference/">
  Spring Shell
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
<script async id="search-script" src="../../_/js/vendor/search.js" data-app-id="WB1FQYI187" data-api-key="c2e84f15fa630d534f1c62b1c413bb77" data-index-name="springdocs" data-stylesheet="../../_/css/vendor/search.css" data-page-version="7.0.9" data-page-generation="7.0" data-page-component="framework"></script>
  </body>
</html>

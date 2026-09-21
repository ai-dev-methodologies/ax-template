<!DOCTYPE html>
<html lang="en">
  <script src="https://cdn.cookielaw.org/scripttemplates/otSDKStub.js" data-domain-script="018ee325-b3a7-7753-937b-b8b3e643b1a7"></script><script>function OptanonWrapper() {}</script><script>function setGTM(w, d, s, l, i) { w[l] = w[l] || []; w[l].push({ "gtm.start": new Date().getTime(), event: "gtm.js"}); var f = d.getElementsByTagName(s)[0], j = d.createElement(s), dl = l != "dataLayer" ? "&l=" + l : ""; j.async = true; j.src = "https://www.googletagmanager.com/gtm.js?id=" + i + dl; f.parentNode.insertBefore(j, f); } if (document.cookie.indexOf("OptanonConsent") > -1 && document.cookie.indexOf("groups=") > -1) { setGTM(window, document, "script", "dataLayer", "GTM-W8CQ8TL"); } else { waitForOnetrustActiveGroups(); } var timer; function waitForOnetrustActiveGroups() { if (document.cookie.indexOf("OptanonConsent") > -1 && document.cookie.indexOf("groups=") > -1) { clearTimeout(timer); setGTM(window, document, "script", "dataLayer", "GTM-W8CQ8TL"); } else { timer = setTimeout(waitForOnetrustActiveGroups, 250); }}</script>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>Task Execution and Scheduling :: Spring Framework</title>
    <link rel="canonical" href="https://docs.spring.io/spring-framework/reference/integration/scheduling.html">
    <link rel="prev" href="email.html">
    <link rel="next" href="cache.html">
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

    <meta name="antora-ui-version" content="v0.4.26"> 
    <meta name="version" content="7.0.9">
    <meta name="generation" content="7.0">
    <meta name="versioned-url" content="https://docs.spring.io/spring-framework/reference/7.0/integration/scheduling.html">
    <meta name="component" content="framework">
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
    <a class="nav-link"  href="../overview.html">Overview</a>
  </li>
  <li class="nav-item" data-depth="1">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../core.html">Core Technologies</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../core/beans.html">The IoC Container</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../core/beans/introduction.html">Introduction to the Spring IoC Container and Beans</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../core/beans/basics.html">Container Overview</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../core/beans/definition.html">Bean Overview</a>
  </li>
  <li class="nav-item" data-depth="3">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../core/beans/dependencies.html">Dependencies</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../core/beans/dependencies/factory-collaborators.html">Dependency Injection</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../core/beans/dependencies/factory-properties-detailed.html">Dependencies and Configuration in Detail</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../core/beans/dependencies/factory-dependson.html">Using <code>depends-on</code></a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../core/beans/dependencies/factory-lazy-init.html">Lazy-initialized Beans</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../core/beans/dependencies/factory-autowire.html">Autowiring Collaborators</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../core/beans/dependencies/factory-method-injection.html">Method Injection</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../core/beans/factory-scopes.html">Bean Scopes</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../core/beans/factory-nature.html">Customizing the Nature of a Bean</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../core/beans/child-bean-definitions.html">Bean Definition Inheritance</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../core/beans/factory-extension.html">Container Extension Points</a>
  </li>
  <li class="nav-item" data-depth="3">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../core/beans/annotation-config.html">Annotation-based Container Configuration</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../core/beans/annotation-config/autowired.html">Using <code>@Autowired</code></a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../core/beans/annotation-config/autowired-primary.html">Fine-tuning Annotation-based Autowiring with <code>@Primary</code> or <code>@Fallback</code></a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../core/beans/annotation-config/autowired-qualifiers.html">Fine-tuning Annotation-based Autowiring with Qualifiers</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../core/beans/annotation-config/generics-as-qualifiers.html">Using Generics as Autowiring Qualifiers</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../core/beans/annotation-config/custom-autowire-configurer.html">Using <code>CustomAutowireConfigurer</code></a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../core/beans/annotation-config/resource.html">Injection with <code>@Resource</code></a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../core/beans/annotation-config/value-annotations.html">Using <code>@Value</code></a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../core/beans/annotation-config/postconstruct-and-predestroy-annotations.html">Using <code>@PostConstruct</code> and <code>@PreDestroy</code></a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../core/beans/classpath-scanning.html">Classpath Scanning and Managed Components</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../core/beans/standard-annotations.html">Using JSR-330 Standard Annotations</a>
  </li>
  <li class="nav-item" data-depth="3">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../core/beans/java.html">Java-based Container Configuration</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../core/beans/java/basic-concepts.html">Basic Concepts: <code>@Bean</code> and <code>@Configuration</code></a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../core/beans/java/instantiating-container.html">Instantiating the Spring Container by Using <code>AnnotationConfigApplicationContext</code></a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../core/beans/java/bean-annotation.html">Using the <code>@Bean</code> Annotation</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../core/beans/java/configuration-annotation.html">Using the <code>@Configuration</code> annotation</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../core/beans/java/composing-configuration-classes.html">Composing Java-based Configurations</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../core/beans/java/programmatic-bean-registration.html">Programmatic Bean Registration</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../core/beans/environment.html">Environment Abstraction</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../core/beans/context-load-time-weaver.html">Registering a <code>LoadTimeWeaver</code></a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../core/beans/context-introduction.html">Additional Capabilities of the <code>ApplicationContext</code></a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../core/beans/beanfactory.html">The <code>BeanFactory</code> API</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../core/resources.html">Resources</a>
  </li>
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../core/validation.html">Validation, Data Binding, and Type Conversion</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../core/validation/validator.html">Validation Using Spring&#8217;s Validator Interface</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../core/validation/data-binding.html">Data Binding</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../core/validation/error-code-resolution.html">Resolving Error Codes to Error Messages</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../core/validation/convert.html">Spring Type Conversion</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../core/validation/format.html">Spring Field Formatting</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../core/validation/format-configuring-formatting-globaldatetimeformat.html">Configuring a Global Date and Time Format</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../core/validation/beanvalidation.html">Java Bean Validation</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../core/expressions.html">Spring Expression Language (SpEL)</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../core/expressions/evaluation.html">Evaluation</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../core/expressions/beandef.html">Expressions in Bean Definitions</a>
  </li>
  <li class="nav-item" data-depth="3">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../core/expressions/language-ref.html">Language Reference</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../core/expressions/language-ref/literal.html">Literal Expressions</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../core/expressions/language-ref/properties-arrays.html">Properties, Arrays, Lists, Maps, and Indexers</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../core/expressions/language-ref/inline-lists.html">Inline Lists</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../core/expressions/language-ref/inline-maps.html">Inline Maps</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../core/expressions/language-ref/array-construction.html">Array Construction</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../core/expressions/language-ref/methods.html">Methods</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../core/expressions/language-ref/operators.html">Operators</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../core/expressions/language-ref/types.html">Types</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../core/expressions/language-ref/constructors.html">Constructors</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../core/expressions/language-ref/variables.html">Variables</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../core/expressions/language-ref/functions.html">Functions</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../core/expressions/language-ref/varargs.html">Varargs Invocations</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../core/expressions/language-ref/bean-references.html">Bean References</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../core/expressions/language-ref/operator-ternary.html">Ternary Operator (If-Then-Else)</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../core/expressions/language-ref/operator-elvis.html">The Elvis Operator</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../core/expressions/language-ref/operator-safe-navigation.html">Safe Navigation Operator</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../core/expressions/language-ref/collection-selection.html">Collection Selection</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../core/expressions/language-ref/collection-projection.html">Collection Projection</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../core/expressions/language-ref/templating.html">Expression Templating</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../core/expressions/example-classes.html">Classes Used in the Examples</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../core/aop.html">Aspect Oriented Programming with Spring</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../core/aop/introduction-defn.html">AOP Concepts</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../core/aop/introduction-spring-defn.html">Spring AOP Capabilities and Goals</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../core/aop/introduction-proxies.html">AOP Proxies</a>
  </li>
  <li class="nav-item" data-depth="3">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../core/aop/ataspectj.html">@AspectJ support</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../core/aop/ataspectj/aspectj-support.html">Enabling @AspectJ Support</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../core/aop/ataspectj/at-aspectj.html">Declaring an Aspect</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../core/aop/ataspectj/pointcuts.html">Declaring a Pointcut</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../core/aop/ataspectj/advice.html">Declaring Advice</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../core/aop/ataspectj/introductions.html">Introductions</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../core/aop/ataspectj/instantiation-models.html">Aspect Instantiation Models</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../core/aop/ataspectj/example.html">An AOP Example</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../core/aop/schema.html">Schema-based AOP Support</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../core/aop/choosing.html">Choosing which AOP Declaration Style to Use</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../core/aop/mixing-styles.html">Mixing Aspect Types</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../core/aop/proxying.html">Proxying Mechanisms</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../core/aop/aspectj-programmatic.html">Programmatic Creation of @AspectJ Proxies</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../core/aop/using-aspectj.html">Using AspectJ with Spring Applications</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../core/aop/resources.html">Further Resources</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../core/aop-api.html">Spring AOP APIs</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../core/aop-api/pointcuts.html">Pointcut API in Spring</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../core/aop-api/advice.html">Advice API in Spring</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../core/aop-api/advisor.html">The Advisor API in Spring</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../core/aop-api/pfb.html">Using the <code>ProxyFactoryBean</code> to Create AOP Proxies</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../core/aop-api/concise-proxy.html">Concise Proxy Definitions</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../core/aop-api/prog.html">Creating AOP Proxies Programmatically with the <code>ProxyFactory</code></a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../core/aop-api/advised.html">Manipulating Advised Objects</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../core/aop-api/autoproxy.html">Using the "auto-proxy" facility</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../core/aop-api/targetsource.html">Using <code>TargetSource</code> Implementations</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../core/aop-api/extensibility.html">Defining New Advice Types</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../core/resilience.html">Resilience Features</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../core/null-safety.html">Null-safety</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../core/databuffer-codec.html">Data Buffers and Codecs</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../core/aot.html">Ahead of Time Optimizations</a>
  </li>
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../core/appendix.html">Appendix</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../core/appendix/xsd-schemas.html">XML Schemas</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../core/appendix/xml-custom.html">XML Schema Authoring</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../core/appendix/application-startup-steps.html">Application Startup Steps</a>
  </li>
</ul>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="1">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../data-access.html">Data Access</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../data-access/transaction.html">Transaction Management</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../data-access/transaction/motivation.html">Advantages of the Spring Framework&#8217;s Transaction Support Model</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../data-access/transaction/strategies.html">Understanding the Spring Framework Transaction Abstraction</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../data-access/transaction/tx-resource-synchronization.html">Synchronizing Resources with Transactions</a>
  </li>
  <li class="nav-item" data-depth="3">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../data-access/transaction/declarative.html">Declarative Transaction Management</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../data-access/transaction/declarative/tx-decl-explained.html">Understanding the Spring Framework&#8217;s Declarative Transaction Implementation</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../data-access/transaction/declarative/first-example.html">Example of Declarative Transaction Implementation</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../data-access/transaction/declarative/rolling-back.html">Rolling Back a Declarative Transaction</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../data-access/transaction/declarative/diff-tx.html">Configuring Different Transactional Semantics for Different Beans</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../data-access/transaction/declarative/txadvice-settings.html">&lt;tx:advice/&gt; Settings</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../data-access/transaction/declarative/annotations.html">Using <code>@Transactional</code></a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../data-access/transaction/declarative/tx-propagation.html">Transaction Propagation</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../data-access/transaction/declarative/applying-more-than-just-tx-advice.html">Advising Transactional Operations</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../data-access/transaction/declarative/aspectj.html">Using <code>@Transactional</code> with AspectJ</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../data-access/transaction/programmatic.html">Programmatic Transaction Management</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../data-access/transaction/tx-decl-vs-prog.html">Choosing Between Programmatic and Declarative Transaction Management</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../data-access/transaction/event.html">Transaction-bound Events</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../data-access/transaction/application-server-integration.html">Application server-specific integration</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../data-access/transaction/solutions-to-common-problems.html">Solutions to Common Problems</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../data-access/transaction/resources.html">Further Resources</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../data-access/dao.html">DAO Support</a>
  </li>
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../data-access/jdbc.html">Data Access with JDBC</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../data-access/jdbc/choose-style.html">Choosing an Approach for JDBC Database Access</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../data-access/jdbc/packages.html">Package Hierarchy</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../data-access/jdbc/core.html">Using the JDBC Core Classes to Control Basic JDBC Processing and Error Handling</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../data-access/jdbc/connections.html">Controlling Database Connections</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../data-access/jdbc/advanced.html">JDBC Batch Operations</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../data-access/jdbc/simple.html">Simplifying JDBC Operations with the <code>SimpleJdbc</code> Classes</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../data-access/jdbc/object.html">Modeling JDBC Operations as Java Objects</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../data-access/jdbc/parameter-handling.html">Common Problems with Parameter and Data Value Handling</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../data-access/jdbc/embedded-database-support.html">Embedded Database Support</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../data-access/jdbc/initializing-datasource.html">Initializing a <code>DataSource</code></a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../data-access/r2dbc.html">Data Access with R2DBC</a>
  </li>
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../data-access/orm.html">Object Relational Mapping (ORM) Data Access</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../data-access/orm/introduction.html">Introduction to ORM with Spring</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../data-access/orm/general.html">General ORM Integration Considerations</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../data-access/orm/hibernate.html">Hibernate</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../data-access/orm/jpa.html">JPA</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../data-access/oxm.html">Marshalling XML by Using Object-XML Mappers</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../data-access/appendix.html">Appendix</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="1">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../web.html">Web on Servlet Stack</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../web/webmvc.html">Spring Web MVC</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../web/webmvc/mvc-servlet.html">DispatcherServlet</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/webmvc/mvc-servlet/context-hierarchy.html">Context Hierarchy</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/webmvc/mvc-servlet/special-bean-types.html">Special Bean Types</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/webmvc/mvc-servlet/config.html">Web MVC Config</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/webmvc/mvc-servlet/container-config.html">Servlet Config</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/webmvc/mvc-servlet/sequence.html">Processing</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/webmvc/mvc-servlet/handlermapping-path.html">Path Matching</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/webmvc/mvc-servlet/handlermapping-interceptor.html">Interception</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/webmvc/mvc-servlet/exceptionhandlers.html">Exceptions</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/webmvc/mvc-servlet/viewresolver.html">View Resolution</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/webmvc/mvc-servlet/localeresolver.html">Locale</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/webmvc/mvc-servlet/multipart.html">Multipart Resolver</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/webmvc/mvc-servlet/logging.html">Logging</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../web/webmvc/filters.html">Filters</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../web/webmvc/message-converters.html">HTTP Message Conversion</a>
  </li>
  <li class="nav-item" data-depth="3">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../web/webmvc/mvc-controller.html">Annotated Controllers</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/webmvc/mvc-controller/ann.html">Declaration</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/webmvc/mvc-controller/ann-requestmapping.html">Mapping Requests</a>
  </li>
  <li class="nav-item" data-depth="4">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../web/webmvc/mvc-controller/ann-methods.html">Handler Methods</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../web/webmvc/mvc-controller/ann-methods/arguments.html">Method Arguments</a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../web/webmvc/mvc-controller/ann-methods/return-types.html">Return Values</a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../web/webmvc/mvc-controller/ann-methods/typeconversion.html">Type Conversion</a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../web/webmvc/mvc-controller/ann-methods/matrix-variables.html">Matrix Variables</a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../web/webmvc/mvc-controller/ann-methods/requestparam.html"><code>@RequestParam</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../web/webmvc/mvc-controller/ann-methods/requestheader.html"><code>@RequestHeader</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../web/webmvc/mvc-controller/ann-methods/cookievalue.html"><code>@CookieValue</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../web/webmvc/mvc-controller/ann-methods/modelattrib-method-args.html"><code>@ModelAttribute</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../web/webmvc/mvc-controller/ann-methods/sessionattributes.html"><code>@SessionAttributes</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../web/webmvc/mvc-controller/ann-methods/sessionattribute.html"><code>@SessionAttribute</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../web/webmvc/mvc-controller/ann-methods/requestattrib.html"><code>@RequestAttribute</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../web/webmvc/mvc-controller/ann-methods/redirecting-passing-data.html">Redirect Attributes</a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../web/webmvc/mvc-controller/ann-methods/flash-attributes.html">Flash Attributes</a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../web/webmvc/mvc-controller/ann-methods/multipart-forms.html">Multipart</a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../web/webmvc/mvc-controller/ann-methods/requestbody.html"><code>@RequestBody</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../web/webmvc/mvc-controller/ann-methods/httpentity.html">HttpEntity</a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../web/webmvc/mvc-controller/ann-methods/responsebody.html"><code>@ResponseBody</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../web/webmvc/mvc-controller/ann-methods/responseentity.html">ResponseEntity</a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../web/webmvc/mvc-controller/ann-methods/jackson.html">Jackson JSON</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/webmvc/mvc-controller/ann-modelattrib-methods.html">Model</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/webmvc/mvc-controller/ann-initbinder.html"><code>@InitBinder</code></a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/webmvc/mvc-controller/ann-validation.html">Validation</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/webmvc/mvc-controller/ann-exceptionhandler.html">Exceptions</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/webmvc/mvc-controller/ann-advice.html">Controller Advice</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../web/webmvc-functional.html">Functional Endpoints</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../web/webmvc/mvc-uri-building.html">URI Links</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../web/webmvc/mvc-ann-async.html">Asynchronous Requests</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../web/webmvc/mvc-range.html">Range Requests</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../web/webmvc/mvc-data-binding.html">Data Binding</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../web/webmvc-cors.html">CORS</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../web/webmvc-versioning.html">API Versioning</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../web/webmvc/mvc-ann-rest-exceptions.html">Error Responses</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../web/webmvc/mvc-security.html">Web Security</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../web/webmvc/mvc-caching.html">HTTP Caching</a>
  </li>
  <li class="nav-item" data-depth="3">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../web/webmvc-view.html">View Technologies</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/webmvc-view/mvc-thymeleaf.html">Thymeleaf</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/webmvc-view/mvc-freemarker.html">FreeMarker</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/webmvc-view/mvc-groovymarkup.html">Groovy Markup</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/webmvc-view/mvc-script.html">Script Views</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/webmvc-view/mvc-fragments.html">HTML Fragments</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/webmvc-view/mvc-jsp.html">JSP and JSTL</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/webmvc-view/mvc-feeds.html">RSS and Atom</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/webmvc-view/mvc-document.html">PDF and Excel</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/webmvc-view/mvc-jackson.html">Jackson</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/webmvc-view/mvc-xml-marshalling.html">XML Marshalling</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/webmvc-view/mvc-xslt.html">XSLT Views</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="3">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../web/webmvc/mvc-config.html">MVC Config</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/webmvc/mvc-config/enable.html">Enable MVC Configuration</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/webmvc/mvc-config/customize.html">MVC Config API</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/webmvc/mvc-config/conversion.html">Type Conversion</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/webmvc/mvc-config/validation.html">Validation</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/webmvc/mvc-config/interceptors.html">Interceptors</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/webmvc/mvc-config/content-negotiation.html">Content Types</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/webmvc/mvc-config/message-converters.html">Message Converters</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/webmvc/mvc-config/view-controller.html">View Controllers</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/webmvc/mvc-config/view-resolvers.html">View Resolvers</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/webmvc/mvc-config/static-resources.html">Static Resources</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/webmvc/mvc-config/default-servlet-handler.html">Default Servlet</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/webmvc/mvc-config/path-matching.html">Path Matching</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/webmvc/mvc-config/api-version.html">API Version</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/webmvc/mvc-config/advanced-java.html">Advanced Java Config</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/webmvc/mvc-config/advanced-xml.html">Advanced XML Config</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../web/webmvc/mvc-http2.html">HTTP/2</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../web/webmvc-client.html">REST Clients</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../web/webmvc-test.html">Testing</a>
  </li>
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../web/websocket.html">WebSockets</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../web/websocket/server.html">WebSocket API</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../web/websocket/fallback.html">SockJS Fallback</a>
  </li>
  <li class="nav-item" data-depth="3">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../web/websocket/stomp.html">STOMP</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/websocket/stomp/overview.html">Overview</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/websocket/stomp/benefits.html">Benefits</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/websocket/stomp/enable.html">Enable STOMP</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/websocket/stomp/server-config.html">WebSocket Transport</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/websocket/stomp/message-flow.html">Flow of Messages</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/websocket/stomp/handle-annotations.html">Annotated Controllers</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/websocket/stomp/handle-send.html">Sending Messages</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/websocket/stomp/handle-simple-broker.html">Simple Broker</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/websocket/stomp/handle-broker-relay.html">External Broker</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/websocket/stomp/handle-broker-relay-configure.html">Connecting to a Broker</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/websocket/stomp/destination-separator.html">Dots as Separators</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/websocket/stomp/authentication.html">Authentication</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/websocket/stomp/authentication-token-based.html">Token Authentication</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/websocket/stomp/authorization.html">Authorization</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/websocket/stomp/user-destination.html">User Destinations</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/websocket/stomp/ordered-messages.html">Order of Messages</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/websocket/stomp/application-context-events.html">Events</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/websocket/stomp/interceptors.html">Interception</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/websocket/stomp/client.html">STOMP Client</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/websocket/stomp/scope.html">WebSocket Scope</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/websocket/stomp/configuration-performance.html">Performance</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/websocket/stomp/stats.html">Monitoring</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/websocket/stomp/testing.html">Testing</a>
  </li>
</ul>
  </li>
</ul>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="1">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../web-reactive.html">Web on Reactive Stack</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../web/webflux.html">Spring WebFlux</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../web/webflux/new-framework.html">Overview</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../web/webflux/reactive-spring.html">Reactive Core</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../web/webflux/dispatcher-handler.html"><code>DispatcherHandler</code></a>
  </li>
  <li class="nav-item" data-depth="3">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../web/webflux/controller.html">Annotated Controllers</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/webflux/controller/ann.html"><code>@Controller</code></a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/webflux/controller/ann-requestmapping.html">Mapping Requests</a>
  </li>
  <li class="nav-item" data-depth="4">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../web/webflux/controller/ann-methods.html">Handler Methods</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../web/webflux/controller/ann-methods/arguments.html">Method Arguments</a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../web/webflux/controller/ann-methods/return-types.html">Return Values</a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../web/webflux/controller/ann-methods/typeconversion.html">Type Conversion</a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../web/webflux/controller/ann-methods/matrix-variables.html">Matrix Variables</a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../web/webflux/controller/ann-methods/requestparam.html"><code>@RequestParam</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../web/webflux/controller/ann-methods/requestheader.html"><code>@RequestHeader</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../web/webflux/controller/ann-methods/cookievalue.html"><code>@CookieValue</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../web/webflux/controller/ann-methods/modelattrib-method-args.html"><code>@ModelAttribute</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../web/webflux/controller/ann-methods/sessionattributes.html"><code>@SessionAttributes</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../web/webflux/controller/ann-methods/sessionattribute.html"><code>@SessionAttribute</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../web/webflux/controller/ann-methods/requestattrib.html"><code>@RequestAttribute</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../web/webflux/controller/ann-methods/multipart-forms.html">Multipart Content</a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../web/webflux/controller/ann-methods/requestbody.html"><code>@RequestBody</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../web/webflux/controller/ann-methods/httpentity.html"><code>HttpEntity</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../web/webflux/controller/ann-methods/responsebody.html"><code>@ResponseBody</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../web/webflux/controller/ann-methods/responseentity.html"><code>ResponseEntity</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../web/webflux/controller/ann-methods/jackson.html">Jackson JSON</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/webflux/controller/ann-modelattrib-methods.html"><code>Model</code></a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/webflux/controller/ann-initbinder.html"><code>DataBinder</code></a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/webflux/controller/ann-validation.html">Validation</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/webflux/controller/ann-exceptions.html">Exceptions</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../web/webflux/controller/ann-advice.html">Controller Advice</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../web/webflux-functional.html">Functional Endpoints</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../web/webflux/uri-building.html">URI Links</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../web/webflux/range.html">Range Requests</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../web/webflux/data-binding.html">Data Binding</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../web/webflux-cors.html">CORS</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../web/webflux-versioning.html">API Versioning</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../web/webflux/ann-rest-exceptions.html">Error Responses</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../web/webflux/security.html">Web Security</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../web/webflux/caching.html">HTTP Caching</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../web/webflux-view.html">View Technologies</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../web/webflux/config.html">WebFlux Config</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../web/webflux/http2.html">HTTP/2</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../web/webflux-webclient.html">WebClient</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../web/webflux-webclient/client-builder.html">Configuration</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../web/webflux-webclient/client-retrieve.html"><code>retrieve()</code></a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../web/webflux-webclient/client-exchange.html">Exchange</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../web/webflux-webclient/client-body.html">Request Body</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../web/webflux-webclient/client-filter.html">Filters</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../web/webflux-webclient/client-attributes.html">Attributes</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../web/webflux-webclient/client-context.html">Context</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../web/webflux-webclient/client-synchronous.html">Synchronous Use</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../web/webflux-webclient/client-testing.html">Testing</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../web/webflux-http-service-client.html">HTTP Service Client</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../web/webflux-websocket.html">WebSockets</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../web/webflux-test.html">Testing</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../rsocket.html">RSocket</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../web/webflux-reactive-libraries.html">Reactive Libraries</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="1">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../testing.html">Testing</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../testing/introduction.html">Introduction to Spring Testing</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../testing/unit.html">Unit Testing</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../testing/integration.html">Integration Testing</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../testing/support-jdbc.html">JDBC Testing Support</a>
  </li>
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../testing/testcontext-framework.html">Spring TestContext Framework</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../testing/testcontext-framework/key-abstractions.html">Key Abstractions</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../testing/testcontext-framework/bootstrapping.html">Bootstrapping the TestContext Framework</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../testing/testcontext-framework/tel-config.html"><code>TestExecutionListener</code> Configuration</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../testing/testcontext-framework/application-events.html">Application Events</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../testing/testcontext-framework/test-execution-events.html">Test Execution Events</a>
  </li>
  <li class="nav-item" data-depth="3">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../testing/testcontext-framework/ctx-management.html">Context Management</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../testing/testcontext-framework/ctx-management/javaconfig.html">Context Configuration with Component Classes</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../testing/testcontext-framework/ctx-management/xml.html">Context Configuration with XML Resources</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../testing/testcontext-framework/ctx-management/groovy.html">Context Configuration with Groovy Scripts</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../testing/testcontext-framework/ctx-management/default-config.html">Default Context Configuration</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../testing/testcontext-framework/ctx-management/mixed-config.html">Mixing Component Classes, XML, and Groovy Scripts</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../testing/testcontext-framework/ctx-management/context-customizers.html">Context Configuration with Context Customizers</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../testing/testcontext-framework/ctx-management/initializers.html">Context Configuration with Context Initializers</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../testing/testcontext-framework/ctx-management/inheritance.html">Context Configuration Inheritance</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../testing/testcontext-framework/ctx-management/env-profiles.html">Context Configuration with Environment Profiles</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../testing/testcontext-framework/ctx-management/property-sources.html">Context Configuration with Test Property Sources</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../testing/testcontext-framework/ctx-management/dynamic-property-sources.html">Context Configuration with Dynamic Property Sources</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../testing/testcontext-framework/ctx-management/web.html">Loading a <code>WebApplicationContext</code></a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../testing/testcontext-framework/ctx-management/web-mocks.html">Working with Web Mocks</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../testing/testcontext-framework/ctx-management/caching.html">Context Caching</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../testing/testcontext-framework/ctx-management/context-pausing.html">Context Pausing</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../testing/testcontext-framework/ctx-management/failure-threshold.html">Context Failure Threshold</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../testing/testcontext-framework/ctx-management/hierarchies.html">Context Hierarchies</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../testing/testcontext-framework/fixture-di.html">Dependency Injection of Test Fixtures</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../testing/testcontext-framework/bean-overriding.html">Bean Overriding in Tests</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../testing/testcontext-framework/web-scoped-beans.html">Testing Request- and Session-scoped Beans</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../testing/testcontext-framework/tx.html">Transaction Management</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../testing/testcontext-framework/executing-sql.html">Executing SQL Scripts</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../testing/testcontext-framework/parallel-test-execution.html">Parallel Test Execution</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../testing/testcontext-framework/support-classes.html">TestContext Framework Support Classes</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../testing/testcontext-framework/aot.html">Ahead of Time Support for Tests</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../testing/webtestclient.html">WebTestClient</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../testing/resttestclient.html">RestTestClient</a>
  </li>
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../testing/mockmvc.html">MockMvc</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../testing/mockmvc/overview.html">Overview</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../testing/mockmvc/setup-options.html">Setup Options</a>
  </li>
  <li class="nav-item" data-depth="3">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../testing/mockmvc/hamcrest.html">Hamcrest Integration</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../testing/mockmvc/hamcrest/static-imports.html">Static Imports</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../testing/mockmvc/hamcrest/setup.html">Configuring MockMvc</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../testing/mockmvc/hamcrest/setup-steps.html">Setup Features</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../testing/mockmvc/hamcrest/requests.html">Performing Requests</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../testing/mockmvc/hamcrest/expectations.html">Defining Expectations</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../testing/mockmvc/hamcrest/async-requests.html">Async Requests</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../testing/mockmvc/hamcrest/vs-streaming-response.html">Streaming Responses</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../testing/mockmvc/hamcrest/filters.html">Filter Registrations</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="3">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../testing/mockmvc/assertj.html">AssertJ Integration</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../testing/mockmvc/assertj/setup.html">Configuring MockMvcTester</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../testing/mockmvc/assertj/requests.html">Performing Requests</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../testing/mockmvc/assertj/assertions.html">Defining Expectations</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../testing/mockmvc/assertj/integration.html">MockMvc integration</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="3">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../testing/mockmvc/htmlunit.html">HtmlUnit Integration</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../testing/mockmvc/htmlunit/why.html">Why HtmlUnit Integration?</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../testing/mockmvc/htmlunit/mah.html">MockMvc and HtmlUnit</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../testing/mockmvc/htmlunit/webdriver.html">MockMvc and WebDriver</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../testing/mockmvc/htmlunit/geb.html">MockMvc and Geb</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../testing/mockmvc/vs-end-to-end-integration-tests.html">MockMvc vs End-to-End Tests</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../testing/mockmvc/resources.html">Further Examples</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../testing/spring-mvc-test-client.html">Testing Client Applications</a>
  </li>
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../testing/appendix.html">Appendix</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../testing/annotations.html">Annotations</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../testing/annotations/integration-standard.html">Standard Annotation Support</a>
  </li>
  <li class="nav-item" data-depth="4">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../testing/annotations/integration-spring.html">Spring Testing Annotations</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../testing/annotations/integration-spring/annotation-bootstrapwith.html"><code>@BootstrapWith</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../testing/annotations/integration-spring/annotation-contextconfiguration.html"><code>@ContextConfiguration</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../testing/annotations/integration-spring/annotation-webappconfiguration.html"><code>@WebAppConfiguration</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../testing/annotations/integration-spring/annotation-contexthierarchy.html"><code>@ContextHierarchy</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../testing/annotations/integration-spring/annotation-contextcustomizerfactories.html"><code>@ContextCustomizerFactories</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../testing/annotations/integration-spring/annotation-activeprofiles.html"><code>@ActiveProfiles</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../testing/annotations/integration-spring/annotation-testpropertysource.html"><code>@TestPropertySource</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../testing/annotations/integration-spring/annotation-dynamicpropertysource.html"><code>@DynamicPropertySource</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../testing/annotations/integration-spring/annotation-testbean.html"><code>@TestBean</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../testing/annotations/integration-spring/annotation-mockitobean.html"><code>@MockitoBean</code> and <code>@MockitoSpyBean</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../testing/annotations/integration-spring/annotation-dirtiescontext.html"><code>@DirtiesContext</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../testing/annotations/integration-spring/annotation-testexecutionlisteners.html"><code>@TestExecutionListeners</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../testing/annotations/integration-spring/annotation-recordapplicationevents.html"><code>@RecordApplicationEvents</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../testing/annotations/integration-spring/annotation-commit.html"><code>@Commit</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../testing/annotations/integration-spring/annotation-rollback.html"><code>@Rollback</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../testing/annotations/integration-spring/annotation-beforetransaction.html"><code>@BeforeTransaction</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../testing/annotations/integration-spring/annotation-aftertransaction.html"><code>@AfterTransaction</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../testing/annotations/integration-spring/annotation-sql.html"><code>@Sql</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../testing/annotations/integration-spring/annotation-sqlconfig.html"><code>@SqlConfig</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../testing/annotations/integration-spring/annotation-sqlmergemode.html"><code>@SqlMergeMode</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../testing/annotations/integration-spring/annotation-sqlgroup.html"><code>@SqlGroup</code></a>
  </li>
  <li class="nav-item" data-depth="5">
    <a class="nav-link"  href="../testing/annotations/integration-spring/annotation-disabledinaotmode.html"><code>@DisabledInAotMode</code></a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../testing/annotations/integration-junit4.html">Spring JUnit 4 Testing Annotations</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../testing/annotations/integration-junit-jupiter.html">Spring JUnit Jupiter Testing Annotations</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../testing/annotations/integration-meta.html">Meta-Annotation Support for Testing</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../testing/resources.html">Further Resources</a>
  </li>
</ul>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="1">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../integration.html">Integration</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="rest-clients.html">REST Clients</a>
  </li>
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="jms.html">JMS (Java Message Service)</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="jms/using.html">Using Spring JMS</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="jms/sending.html">Sending a Message</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="jms/receiving.html">Receiving a Message</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="jms/jca-message-endpoint-manager.html">Support for JCA Message Endpoints</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="jms/annotated.html">Annotation-driven Listener Endpoints</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="jms/namespace.html">JMS Namespace Support</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="jmx.html">JMX</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="jmx/exporting.html">Exporting Your Beans to JMX</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="jmx/interface.html">Controlling the Management Interface of Your Beans</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="jmx/naming.html">Controlling  <code>ObjectName</code> Instances for Your Beans</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="jmx/jsr160.html">Using JSR-160 Connectors</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="jmx/proxy.html">Accessing MBeans through Proxies</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="jmx/notifications.html">Notifications</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="jmx/resources.html">Further Resources</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="email.html">Email</a>
  </li>
  <li class="nav-item is-current-page" data-depth="2">
    <a class="nav-link"  href="scheduling.html">Task Execution and Scheduling</a>
  </li>
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="cache.html">Cache Abstraction</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="cache/strategies.html">Understanding the Cache Abstraction</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="cache/annotations.html">Declarative Annotation-based Caching</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="cache/jsr-107.html">JCache (JSR-107) Annotations</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="cache/declarative-xml.html">Declarative XML-based Caching</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="cache/store-configuration.html">Configuring the Cache Storage</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="cache/plug.html">Plugging-in Different Back-end Caches</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="cache/specific-config.html">How can I Set the TTL/TTI/Eviction policy/XXX feature?</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="observability.html">Observability Support</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="aot-cache.html">JVM AOT Cache</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="checkpoint-restore.html">JVM Checkpoint Restore</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="appendix.html">Appendix</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="1">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../languages.html">Language Support</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../languages/kotlin.html">Kotlin</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../languages/kotlin/requirements.html">Requirements</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../languages/kotlin/extensions.html">Extensions</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../languages/kotlin/null-safety.html">Null-safety</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../languages/kotlin/classes-interfaces.html">Classes and Interfaces</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../languages/kotlin/annotations.html">Annotations</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../languages/kotlin/bean-registration-dsl.html">Bean Registration DSL</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../languages/kotlin/web.html">Web</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../languages/kotlin/coroutines.html">Coroutines</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../languages/kotlin/spring-projects-in.html">Spring Projects in Kotlin</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../languages/kotlin/getting-started.html">Getting Started</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../languages/kotlin/resources.html">Resources</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../languages/groovy.html">Apache Groovy</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="1">
    <a class="nav-link"  href="../appendix.html">Appendix</a>
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
      data-title="Task Execution and Scheduling"
      data-levels="2"
    >
      <div class="toc-menu"></div>
    </div>
    <div class="sidebar-links">
        <a href="https://github.com/spring-projects/spring-framework/blob/v7.0.9/framework-docs/modules/ROOT/pages/integration/scheduling.adoc">
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
      <li><a href="../index.html">Spring Framework</a></li>
      <li><a href="../integration.html">Integration</a></li>
      <li><a href="scheduling.html">Task Execution and Scheduling</a></li>
    </ul>
  </nav>
</div><h1 id="page-title" class="page">Task Execution and Scheduling</h1>
<div id="preamble">
<div class="sectionbody">
<div class="paragraph">
<p>The Spring Framework provides abstractions for the asynchronous execution and scheduling of
tasks with the <code>TaskExecutor</code> and <code>TaskScheduler</code> interfaces, respectively. Spring also
features implementations of those interfaces that support thread pools or delegation to
CommonJ within an application server environment. Ultimately, the use of these
implementations behind the common interfaces abstracts away the differences between
Java SE and Jakarta EE environments.</p>
</div>
<div class="paragraph">
<p>Spring also features integration classes to support scheduling with the
<a href="https://www.quartz-scheduler.org/">Quartz Scheduler</a>.</p>
</div>
</div>
</div>
<div class="sect1">
<h2 id="scheduling-task-executor"><a class="anchor" href="#scheduling-task-executor"></a>The Spring <code>TaskExecutor</code> Abstraction</h2>
<div class="sectionbody">
<div class="paragraph">
<p>Executors are the JDK name for the concept of thread pools. The &#8220;executor&#8221; naming is
due to the fact that there is no guarantee that the underlying implementation is
actually a pool. An executor may be single-threaded or even synchronous. Spring&#8217;s
abstraction hides implementation details between the Java SE and Jakarta EE environments.</p>
</div>
<div class="paragraph">
<p>Spring&#8217;s <code>TaskExecutor</code> interface is identical to the <code>java.util.concurrent.Executor</code>
interface. In fact, originally, its primary reason for existence was to abstract away
the need for Java 5 when using thread pools. The interface has a single method
(<code>execute(Runnable task)</code>) that accepts a task for execution based on the semantics
and configuration of the thread pool.</p>
</div>
<div class="paragraph">
<p>The <code>TaskExecutor</code> was originally created to give other Spring components an abstraction
for thread pooling where needed. Components such as the <code>ApplicationEventMulticaster</code>,
JMS&#8217;s <code>AbstractMessageListenerContainer</code>, and Quartz integration all use the
<code>TaskExecutor</code> abstraction to pool threads. However, if your beans need thread pooling
behavior, you can also use this abstraction for your own needs.</p>
</div>
<div class="sect2">
<h3 id="scheduling-task-executor-types"><a class="anchor" href="#scheduling-task-executor-types"></a><code>TaskExecutor</code> Types</h3>
<div class="paragraph">
<p>Spring includes a number of pre-built implementations of <code>TaskExecutor</code>.
In all likelihood, you should never need to implement your own.
The variants that Spring provides are as follows:</p>
</div>
<div class="ulist">
<ul>
<li>
<p><code>SyncTaskExecutor</code>:
This implementation does not run invocations asynchronously. Instead, each
invocation takes place in the calling thread. It is primarily used in situations
where multi-threading is not necessary, such as in simple test cases.</p>
</li>
<li>
<p><code>SimpleAsyncTaskExecutor</code>:
This implementation does not reuse any threads. Rather, it starts up a new thread
for each invocation. However, it does support a concurrency limit that blocks
any invocations that are over the limit until a slot has been freed up. If you
are looking for true pooling, see <code>ThreadPoolTaskExecutor</code>, later in this list.
This will use JDK 21&#8217;s Virtual Threads, when the "virtualThreads"
option is enabled. This implementation also supports graceful shutdown through
Spring&#8217;s lifecycle management.</p>
</li>
<li>
<p><code>ConcurrentTaskExecutor</code>:
This implementation is an adapter for a <code>java.util.concurrent.Executor</code> instance.
There is an alternative (<code>ThreadPoolTaskExecutor</code>) that exposes the <code>Executor</code>
configuration parameters as bean properties. There is rarely a need to use
<code>ConcurrentTaskExecutor</code> directly. However, if the <code>ThreadPoolTaskExecutor</code> is not
flexible enough for your needs, <code>ConcurrentTaskExecutor</code> is an alternative.</p>
</li>
<li>
<p><code>ThreadPoolTaskExecutor</code>:
This implementation is most commonly used. It exposes bean properties for configuring
a <code>java.util.concurrent.ThreadPoolExecutor</code> and wraps it in a <code>TaskExecutor</code>.
If you need to adapt to a different kind of <code>java.util.concurrent.Executor</code>,
we recommend that you use a <code>ConcurrentTaskExecutor</code> instead.
It also provides a pause/resume capability and graceful shutdown through
Spring&#8217;s lifecycle management.</p>
</li>
<li>
<p><code>DefaultManagedTaskExecutor</code>:
This implementation uses a JNDI-obtained <code>ManagedExecutorService</code> in a JSR-236
compatible runtime environment (such as a Jakarta EE application server),
replacing a CommonJ WorkManager for that purpose.</p>
</li>
</ul>
</div>
</div>
<div class="sect2">
<h3 id="scheduling-task-executor-usage"><a class="anchor" href="#scheduling-task-executor-usage"></a>Using a <code>TaskExecutor</code></h3>
<div class="paragraph">
<p>Spring&#8217;s <code>TaskExecutor</code> implementations are commonly used with dependency injection.
In the following example, we define a bean that uses the <code>ThreadPoolTaskExecutor</code>
to asynchronously print out a set of messages:</p>
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
</ul>
</div>
<div id="_tabs_1_java--panel" class="tabpanel" aria-labelledby="_tabs_1_java">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">public class TaskExecutorExample {

	private class MessagePrinterTask implements Runnable {

		private String message;

		public MessagePrinterTask(String message) {
			this.message = message;
		}

		public void run() {
			System.out.println(message);
		}
	}

	private TaskExecutor taskExecutor;

	public TaskExecutorExample(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	public void printMessages() {
		for(int i = 0; i &lt; 25; i++) {
			taskExecutor.execute(new MessagePrinterTask("Message" + i));
		}
	}
}</code></pre>
</div>
</div>
</div>
<div id="_tabs_1_kotlin--panel" class="tabpanel" aria-labelledby="_tabs_1_kotlin">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-kotlin hljs" data-lang="kotlin">class TaskExecutorExample(private val taskExecutor: TaskExecutor) {

	private inner class MessagePrinterTask(private val message: String) : Runnable {
		override fun run() {
			println(message)
		}
	}

	fun printMessages() {
		for (i in 0..24) {
			taskExecutor.execute(
				MessagePrinterTask(
					"Message$i"
				)
			)
		}
	}
}</code></pre>
</div>
</div>
</div>
</div>
</div>
<div class="paragraph">
<p>As you can see, rather than retrieving a thread from the pool and executing it yourself,
you add your <code>Runnable</code> to the queue. Then the <code>TaskExecutor</code> uses its internal rules to
decide when the task gets run.</p>
</div>
<div class="paragraph">
<p>To configure the rules that the <code>TaskExecutor</code> uses, we expose simple bean properties:</p>
</div>
<div id="_tabs_2" class="openblock tabs is-sync is-loading">
<div class="content">
<div class="ulist tablist">
<ul>
<li id="_tabs_2_java" class="tab">
<p>Java</p>
</li>
<li id="_tabs_2_kotlin" class="tab">
<p>Kotlin</p>
</li>
<li id="_tabs_2_xml" class="tab">
<p>Xml</p>
</li>
</ul>
</div>
<div id="_tabs_2_java--panel" class="tabpanel" aria-labelledby="_tabs_2_java">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Bean
ThreadPoolTaskExecutor taskExecutor() {
	ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
	taskExecutor.setCorePoolSize(5);
	taskExecutor.setMaxPoolSize(10);
	taskExecutor.setQueueCapacity(25);
	return taskExecutor;
}

@Bean
TaskExecutorExample taskExecutorExample(ThreadPoolTaskExecutor taskExecutor) {
	return new TaskExecutorExample(taskExecutor);
}</code></pre>
</div>
</div>
</div>
<div id="_tabs_2_kotlin--panel" class="tabpanel" aria-labelledby="_tabs_2_kotlin">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-kotlin hljs" data-lang="kotlin">@Bean
fun taskExecutor() = ThreadPoolTaskExecutor().apply {
	corePoolSize = 5
	maxPoolSize = 10
	queueCapacity = 25
}

@Bean
fun taskExecutorExample(taskExecutor: ThreadPoolTaskExecutor) = TaskExecutorExample(taskExecutor)</code></pre>
</div>
</div>
</div>
<div id="_tabs_2_xml--panel" class="tabpanel" aria-labelledby="_tabs_2_xml">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-xml hljs" data-lang="xml">&lt;bean id="taskExecutor" class="org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor"&gt;
	&lt;property name="corePoolSize" value="5"/&gt;
	&lt;property name="maxPoolSize" value="10"/&gt;
	&lt;property name="queueCapacity" value="25"/&gt;
&lt;/bean&gt;

&lt;bean id="taskExecutorExample" class="TaskExecutorExample"&gt;
	&lt;constructor-arg ref="taskExecutor"/&gt;
&lt;/bean&gt;</code></pre>
</div>
</div>
</div>
</div>
</div>
<div class="paragraph">
<p>Most <code>TaskExecutor</code> implementations provide a way to automatically wrap tasks submitted
with a <code>TaskDecorator</code>. Decorators should delegate to the task it is wrapping, possibly
implementing custom behavior before/after the execution of the task.</p>
</div>
<div class="paragraph">
<p>Let&#8217;s consider a simple implementation that will log messages before and after the execution
or our tasks:</p>
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
</ul>
</div>
<div id="_tabs_3_java--panel" class="tabpanel" aria-labelledby="_tabs_3_java">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java"><span class="fold-block is-hidden-folded">import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.task.TaskDecorator;

</span><span class="fold-block">public class LoggingTaskDecorator implements TaskDecorator {

	private static final Log logger = LogFactory.getLog(LoggingTaskDecorator.class);

	@Override
	public Runnable decorate(Runnable runnable) {
		return () -&gt; {
			logger.debug("Before execution of " + runnable);
			runnable.run();
			logger.debug("After execution of " + runnable);
		};
	}
}</span></code></pre>
</div>
</div>
</div>
<div id="_tabs_3_kotlin--panel" class="tabpanel" aria-labelledby="_tabs_3_kotlin">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-kotlin hljs" data-lang="kotlin"><span class="fold-block is-hidden-folded">import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.springframework.core.task.TaskDecorator

</span><span class="fold-block">class LoggingTaskDecorator : TaskDecorator {

	override fun decorate(runnable: Runnable): Runnable {
		return Runnable {
			logger.debug("Before execution of $runnable")
			runnable.run()
			logger.debug("After execution of $runnable")
		}
	}

	companion object {
		private val logger: Log = LogFactory.getLog(
			LoggingTaskDecorator::class.java
		)
	}
}</span></code></pre>
</div>
</div>
</div>
</div>
</div>
<div class="paragraph">
<p>We can then configure our decorator on a <code>TaskExecutor</code> instance:</p>
</div>
<div id="_tabs_4" class="openblock tabs is-sync is-loading">
<div class="content">
<div class="ulist tablist">
<ul>
<li id="_tabs_4_java" class="tab">
<p>Java</p>
</li>
<li id="_tabs_4_kotlin" class="tab">
<p>Kotlin</p>
</li>
<li id="_tabs_4_xml" class="tab">
<p>Xml</p>
</li>
</ul>
</div>
<div id="_tabs_4_java--panel" class="tabpanel" aria-labelledby="_tabs_4_java">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Bean
ThreadPoolTaskExecutor decoratedTaskExecutor() {
	ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
	taskExecutor.setTaskDecorator(new LoggingTaskDecorator());
	return taskExecutor;
}</code></pre>
</div>
</div>
</div>
<div id="_tabs_4_kotlin--panel" class="tabpanel" aria-labelledby="_tabs_4_kotlin">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-kotlin hljs" data-lang="kotlin">@Bean
fun decoratedTaskExecutor() = ThreadPoolTaskExecutor().apply {
	setTaskDecorator(LoggingTaskDecorator())
}</code></pre>
</div>
</div>
</div>
<div id="_tabs_4_xml--panel" class="tabpanel" aria-labelledby="_tabs_4_xml">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-xml hljs" data-lang="xml">&lt;bean id="decoratedTaskExecutor" class="org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor"&gt;
	&lt;property name="taskDecorator" ref="loggingTaskDecorator"/&gt;
&lt;/bean&gt;</code></pre>
</div>
</div>
</div>
</div>
</div>
<div class="paragraph">
<p>In case multiple decorators are needed, the <code>org.springframework.core.task.support.CompositeTaskDecorator</code>
can be used to execute sequentially multiple decorators.</p>
</div>
</div>
</div>
</div>
<div class="sect1">
<h2 id="scheduling-task-scheduler"><a class="anchor" href="#scheduling-task-scheduler"></a>The Spring <code>TaskScheduler</code> Abstraction</h2>
<div class="sectionbody">
<div class="paragraph">
<p>In addition to the <code>TaskExecutor</code> abstraction, Spring has a <code>TaskScheduler</code> SPI with a
variety of methods for scheduling tasks to run at some point in the future. The following
listing shows the <code>TaskScheduler</code> interface definition:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">public interface TaskScheduler {

	Clock getClock();

	ScheduledFuture schedule(Runnable task, Trigger trigger);

	ScheduledFuture schedule(Runnable task, Instant startTime);

	ScheduledFuture scheduleAtFixedRate(Runnable task, Instant startTime, Duration period);

	ScheduledFuture scheduleAtFixedRate(Runnable task, Duration period);

	ScheduledFuture scheduleWithFixedDelay(Runnable task, Instant startTime, Duration delay);

	ScheduledFuture scheduleWithFixedDelay(Runnable task, Duration delay);</code></pre>
</div>
</div>
<div class="paragraph">
<p>The simplest method is the one named <code>schedule</code> that takes only a <code>Runnable</code> and an <code>Instant</code>.
That causes the task to run once after the specified time. All of the other methods
are capable of scheduling tasks to run repeatedly. The fixed-rate and fixed-delay
methods are for simple, periodic execution, but the method that accepts a <code>Trigger</code> is
much more flexible.</p>
</div>
<div class="sect2">
<h3 id="scheduling-trigger-interface"><a class="anchor" href="#scheduling-trigger-interface"></a><code>Trigger</code> Interface</h3>
<div class="paragraph">
<p>The <code>Trigger</code> interface is essentially inspired by JSR-236. The basic idea of the
<code>Trigger</code> is that execution times may be determined based on past execution outcomes or
even arbitrary conditions. If these determinations take into account the outcome of the
preceding execution, that information is available within a <code>TriggerContext</code>. The
<code>Trigger</code> interface itself is quite simple, as the following listing shows:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">public interface Trigger {

	Instant nextExecution(TriggerContext triggerContext);
}</code></pre>
</div>
</div>
<div class="paragraph">
<p>The <code>TriggerContext</code> is the most important part. It encapsulates all of
the relevant data and is open for extension in the future, if necessary. The
<code>TriggerContext</code> is an interface (a <code>SimpleTriggerContext</code> implementation is used by
default). The following listing shows the available methods for <code>Trigger</code> implementations.</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">public interface TriggerContext {

	Clock getClock();

	Instant lastScheduledExecution();

	Instant lastActualExecution();

	Instant lastCompletion();
}</code></pre>
</div>
</div>
</div>
<div class="sect2">
<h3 id="scheduling-trigger-implementations"><a class="anchor" href="#scheduling-trigger-implementations"></a><code>Trigger</code> Implementations</h3>
<div class="paragraph">
<p>Spring provides two implementations of the <code>Trigger</code> interface. The most interesting one
is the <code>CronTrigger</code>. It enables the scheduling of tasks based on
<a href="#scheduling-cron-expression">cron expressions</a>.
For example, the following task is scheduled to run 15 minutes past each hour but only
during the 9-to-5 "business hours" on weekdays:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">scheduler.schedule(task, new CronTrigger("0 15 9-17 * * MON-FRI"));</code></pre>
</div>
</div>
<div class="paragraph">
<p>The other implementation is a <code>PeriodicTrigger</code> that accepts a fixed
period, an optional initial delay value, and a boolean to indicate whether the period
should be interpreted as a fixed-rate or a fixed-delay. Since the <code>TaskScheduler</code>
interface already defines methods for scheduling tasks at a fixed rate or with a
fixed delay, those methods should be used directly whenever possible. The value of the
<code>PeriodicTrigger</code> implementation is that you can use it within components that rely on
the <code>Trigger</code> abstraction. For example, it may be convenient to allow periodic triggers,
cron-based triggers, and even custom trigger implementations to be used interchangeably.
Such a component could take advantage of dependency injection so that you can configure
such <code>Triggers</code> externally and, therefore, easily modify or extend them.</p>
</div>
</div>
<div class="sect2">
<h3 id="scheduling-task-scheduler-implementations"><a class="anchor" href="#scheduling-task-scheduler-implementations"></a><code>TaskScheduler</code> implementations</h3>
<div class="paragraph">
<p>As with Spring&#8217;s <code>TaskExecutor</code> abstraction, the primary benefit of the <code>TaskScheduler</code>
arrangement is that an application&#8217;s scheduling needs are decoupled from the deployment
environment. This abstraction level is particularly relevant when deploying to an
application server environment where threads should not be created directly by the
application itself. For such scenarios, Spring provides a <code>DefaultManagedTaskScheduler</code>
that delegates to a JSR-236 <code>ManagedScheduledExecutorService</code> in a Jakarta EE environment.</p>
</div>
<div class="paragraph">
<p>Whenever external thread management is not a requirement, a simpler alternative is
a local <code>ScheduledExecutorService</code> setup within the application, which can be adapted
through Spring&#8217;s <code>ConcurrentTaskScheduler</code>. As a convenience, Spring also provides a
<code>ThreadPoolTaskScheduler</code>, which internally delegates to a <code>ScheduledExecutorService</code>
to provide common bean-style configuration along the lines of <code>ThreadPoolTaskExecutor</code>.
These variants work perfectly fine for locally embedded thread pool setups in lenient
application server environments, as well&#8201;&#8212;&#8201;in particular on Tomcat and Jetty.</p>
</div>
<div class="paragraph">
<p>As of 6.1, <code>ThreadPoolTaskScheduler</code> provides a pause/resume capability and graceful
shutdown through Spring&#8217;s lifecycle management. There is also a new option called
<code>SimpleAsyncTaskScheduler</code> which is aligned with JDK 21&#8217;s Virtual Threads, using a
single scheduler thread but firing up a new thread for every scheduled task execution
(except for fixed-delay tasks which all operate on a single scheduler thread, so for
this virtual-thread-aligned option, fixed rates and cron triggers are recommended).</p>
</div>
</div>
</div>
</div>
<div class="sect1">
<h2 id="scheduling-annotation-support"><a class="anchor" href="#scheduling-annotation-support"></a>Annotation Support for Scheduling and Asynchronous Execution</h2>
<div class="sectionbody">
<div class="paragraph">
<p>Spring provides annotation support for both task scheduling and asynchronous method
execution.</p>
</div>
<div class="sect2">
<h3 id="scheduling-enable-annotation-support"><a class="anchor" href="#scheduling-enable-annotation-support"></a>Enable Scheduling Annotations</h3>
<div class="paragraph">
<p>To enable support for <code>@Scheduled</code> and <code>@Async</code> annotations, you can add <code>@EnableScheduling</code>
and <code>@EnableAsync</code> to one of your <code>@Configuration</code> classes, or <code>&lt;task:annotation-driven&gt;</code> element,
as the following example shows:</p>
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
<li id="_tabs_5_xml" class="tab">
<p>Xml</p>
</li>
</ul>
</div>
<div id="_tabs_5_java--panel" class="tabpanel" aria-labelledby="_tabs_5_java">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Configuration
@EnableAsync
@EnableScheduling
public class SchedulingConfiguration {
}</code></pre>
</div>
</div>
</div>
<div id="_tabs_5_kotlin--panel" class="tabpanel" aria-labelledby="_tabs_5_kotlin">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-kotlin hljs" data-lang="kotlin">@Configuration
@EnableAsync
@EnableScheduling
class SchedulingConfiguration</code></pre>
</div>
</div>
</div>
<div id="_tabs_5_xml--panel" class="tabpanel" aria-labelledby="_tabs_5_xml">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-xml hljs" data-lang="xml">&lt;beans xmlns="http://www.springframework.org/schema/beans"
	   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:task="http://www.springframework.org/schema/task"
	   xsi:schemaLocation="http://www.springframework.org/schema/beans
	   https://www.springframework.org/schema/beans/spring-beans.xsd
	   http://www.springframework.org/schema/task
	   https://www.springframework.org/schema/task/spring-task.xsd"&gt;

	&lt;task:annotation-driven executor="myExecutor" scheduler="myScheduler"/&gt;
	&lt;task:executor id="myExecutor" pool-size="5"/&gt;
	&lt;task:scheduler id="myScheduler" pool-size="10"/&gt;
&lt;/beans&gt;</code></pre>
</div>
</div>
</div>
</div>
</div>
<div class="paragraph">
<p>You can pick and choose the relevant annotations for your application. For example,
if you need only support for <code>@Scheduled</code>, you can omit <code>@EnableAsync</code>. For more
fine-grained control, you can additionally implement the <code>SchedulingConfigurer</code>
interface, the <code>AsyncConfigurer</code> interface, or both. See the
<a href="https://docs.spring.io/spring-framework/docs/7.0.9/javadoc-api/org/springframework/scheduling/annotation/SchedulingConfigurer.html"><code>SchedulingConfigurer</code></a>
and <a href="https://docs.spring.io/spring-framework/docs/7.0.9/javadoc-api/org/springframework/scheduling/annotation/AsyncConfigurer.html"><code>AsyncConfigurer</code></a>
javadoc for full details.</p>
</div>
<div class="paragraph">
<p>Note that, with the preceding XML, an executor reference is provided for handling those
tasks that correspond to methods with the <code>@Async</code> annotation, and the scheduler
reference is provided for managing those methods annotated with <code>@Scheduled</code>.</p>
</div>
<div class="admonitionblock note">
<table>
<tr>
<td class="icon">
<i class="fa icon-note" title="Note"></i>
</td>
<td class="content">
The default advice mode for processing <code>@Async</code> annotations is <code>proxy</code> which allows
for interception of calls through the proxy only. Local calls within the same class
cannot get intercepted that way. For a more advanced mode of interception, consider
switching to <code>aspectj</code> mode in combination with compile-time or load-time weaving.
</td>
</tr>
</table>
</div>
</div>
<div class="sect2">
<h3 id="scheduling-annotation-support-scheduled"><a class="anchor" href="#scheduling-annotation-support-scheduled"></a>The <code>@Scheduled</code> annotation</h3>
<div class="paragraph">
<p>You can add the <code>@Scheduled</code> annotation to a method, along with trigger metadata. For
example, the following method is invoked every five seconds (5000 milliseconds) with a
fixed delay, meaning that the period is measured from the completion time of each
preceding invocation.</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Scheduled(fixedDelay = 5000)
public void doSomething() {
	// something that should run periodically
}</code></pre>
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
<p>By default, milliseconds will be used as the time unit for fixed delay, fixed rate, and
initial delay values. If you would like to use a different time unit such as seconds or
minutes, you can configure this via the <code>timeUnit</code> attribute in <code>@Scheduled</code>.</p>
</div>
<div class="paragraph">
<p>For example, the previous example can also be written as follows.</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Scheduled(fixedDelay = 5, timeUnit = TimeUnit.SECONDS)
public void doSomething() {
	// something that should run periodically
}</code></pre>
</div>
</div>
</td>
</tr>
</table>
</div>
<div class="paragraph">
<p>If you need a fixed-rate execution, you can use the <code>fixedRate</code> attribute within the
annotation. The following method is invoked every five seconds (measured between the
successive start times of each invocation):</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Scheduled(fixedRate = 5, timeUnit = TimeUnit.SECONDS)
public void doSomething() {
	// something that should run periodically
}</code></pre>
</div>
</div>
<div class="paragraph">
<p>For fixed-delay and fixed-rate tasks, you can specify an initial delay by indicating
the amount of time to wait before the first execution of the method, as the following
<code>fixedRate</code> example shows:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Scheduled(initialDelay = 1000, fixedRate = 5000)
public void doSomething() {
	// something that should run periodically
}</code></pre>
</div>
</div>
<div class="paragraph">
<p>For one-time tasks, you can just specify an initial delay by indicating the amount
of time to wait before the intended execution of the method:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Scheduled(initialDelay = 1000)
public void doSomething() {
	// something that should run only once
}</code></pre>
</div>
</div>
<div class="paragraph">
<p>If simple periodic scheduling is not expressive enough, you can provide a
<a href="#scheduling-cron-expression">cron expression</a>.
The following example runs only on weekdays:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Scheduled(cron="*/5 * * * * MON-FRI")
public void doSomething() {
	// something that should run on weekdays only
}</code></pre>
</div>
</div>
<div class="admonitionblock tip">
<table>
<tr>
<td class="icon">
<i class="fa icon-tip" title="Tip"></i>
</td>
<td class="content">
You can also use the <code>zone</code> attribute to specify the time zone in which the cron
expression is resolved.
</td>
</tr>
</table>
</div>
<div class="paragraph">
<p>Notice that the methods to be scheduled must have void returns and must not accept any
arguments. If the method needs to interact with other objects from the application
context, those would typically have been provided through dependency injection.</p>
</div>
<div class="paragraph">
<p><code>@Scheduled</code> can be used as a repeatable annotation. If several scheduled declarations
are found on the same method, each of them will be processed independently, with a
separate trigger firing for each of them. As a consequence, such co-located schedules
may overlap and execute multiple times in parallel or in immediate succession.
Please make sure that your specified cron expressions etc do not accidentally overlap.</p>
</div>
<div class="admonitionblock note">
<table>
<tr>
<td class="icon">
<i class="fa icon-note" title="Note"></i>
</td>
<td class="content">
<div class="paragraph">
<p>As of Spring Framework 4.3, <code>@Scheduled</code> methods are supported on beans of any scope.</p>
</div>
<div class="paragraph">
<p>Make sure that you are not initializing multiple instances of the same <code>@Scheduled</code>
annotation class at runtime, unless you do want to schedule callbacks to each such
instance. Related to this, make sure that you do not use <code>@Configurable</code> on bean
classes that are annotated with <code>@Scheduled</code> and registered as regular Spring beans
with the container. Otherwise, you would get double initialization (once through the
container and once through the <code>@Configurable</code> aspect), with the consequence of each
<code>@Scheduled</code> method being invoked twice.</p>
</div>
</td>
</tr>
</table>
</div>
</div>
<div class="sect2">
<h3 id="scheduling-annotation-support-scheduled-reactive"><a class="anchor" href="#scheduling-annotation-support-scheduled-reactive"></a>The <code>@Scheduled</code> annotation on Reactive methods or Kotlin suspending functions</h3>
<div class="paragraph">
<p>As of Spring Framework 6.1, <code>@Scheduled</code> methods are also supported on several types
of reactive methods:</p>
</div>
<div class="ulist">
<ul>
<li>
<p>methods with a <code>Publisher</code> return type (or any concrete implementation of <code>Publisher</code>)
like in the following example:</p>
</li>
</ul>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Scheduled(fixedDelay = 500)
public Publisher&lt;Void&gt; reactiveSomething() {
	// return an instance of Publisher
}</code></pre>
</div>
</div>
<div class="ulist">
<ul>
<li>
<p>methods with a return type that can be adapted to <code>Publisher</code> via the shared instance
of the <code>ReactiveAdapterRegistry</code>, provided the type supports <em>deferred subscription</em> like
in the following example:</p>
</li>
</ul>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Scheduled(fixedDelay = 500)
public Single&lt;String&gt; rxjavaNonPublisher() {
	return Single.just("example");
}</code></pre>
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
<p>The <code>CompletableFuture</code> class is an example of a type that can typically be adapted
to <code>Publisher</code> but doesn&#8217;t support deferred subscription. Its <code>ReactiveAdapter</code> in the
registry denotes that by having the <code>getDescriptor().isDeferred()</code> method return <code>false</code>.</p>
</div>
</td>
</tr>
</table>
</div>
<div class="ulist">
<ul>
<li>
<p>Kotlin suspending functions, like in the following example:</p>
</li>
</ul>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-kotlin hljs" data-lang="kotlin">@Scheduled(fixedDelay = 500)
suspend fun something() {
	// do something asynchronous
}</code></pre>
</div>
</div>
<div class="ulist">
<ul>
<li>
<p>methods that return a Kotlin <code>Flow</code> or <code>Deferred</code> instance, like in the following example:</p>
</li>
</ul>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-kotlin hljs" data-lang="kotlin">@Scheduled(fixedDelay = 500)
fun something(): Flow&lt;Void&gt; {
	flow {
		// do something asynchronous
	}
}</code></pre>
</div>
</div>
<div class="paragraph">
<p>All these types of methods must be declared without any arguments. In the case of Kotlin
suspending functions, the <code>kotlinx.coroutines.reactor</code> bridge must also be present to allow
the framework to invoke a suspending function as a <code>Publisher</code>.</p>
</div>
<div class="paragraph">
<p>The Spring Framework will obtain a <code>Publisher</code> for the annotated method once and will
schedule a <code>Runnable</code> in which it subscribes to said <code>Publisher</code>. These inner regular
subscriptions occur according to the corresponding <code>cron</code>/<code>fixedDelay</code>/<code>fixedRate</code> configuration.</p>
</div>
<div class="paragraph">
<p>If the <code>Publisher</code> emits <code>onNext</code> signal(s), these are ignored and discarded (the same way
return values from synchronous <code>@Scheduled</code> methods are ignored).</p>
</div>
<div class="paragraph">
<p>In the following example, the <code>Flux</code> emits <code>onNext("Hello")</code>, <code>onNext("World")</code> every 5
seconds, but these values are unused:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Scheduled(initialDelay = 5000, fixedRate = 5000)
public Flux&lt;String&gt; reactiveSomething() {
	return Flux.just("Hello", "World");
}</code></pre>
</div>
</div>
<div class="paragraph">
<p>If the <code>Publisher</code> emits an <code>onError</code> signal, it is logged at <code>WARN</code> level and recovered.
Because of the asynchronous and lazy nature of <code>Publisher</code> instances, exceptions are
not thrown from the <code>Runnable</code> task: this means that the <code>ErrorHandler</code> contract is not
involved for reactive methods.</p>
</div>
<div class="paragraph">
<p>As a result, further scheduled subscription occurs despite the error.</p>
</div>
<div class="paragraph">
<p>In the following example, the <code>Mono</code> subscription fails twice in the first five seconds.
Then subscriptions start succeeding, printing a message to the standard output every five
seconds:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Scheduled(initialDelay = 0, fixedRate = 5000)
public Mono&lt;Void&gt; reactiveSomething() {
	AtomicInteger countdown = new AtomicInteger(2);

	return Mono.defer(() -&gt; {
		if (countDown.get() == 0 || countDown.decrementAndGet() == 0) {
			return Mono.fromRunnable(() -&gt; System.out.println("Message"));
		}
		return Mono.error(new IllegalStateException("Cannot deliver message"));
	})
}</code></pre>
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
<p>When destroying the annotated bean or closing the application context, Spring Framework cancels
scheduled tasks, which includes the next scheduled subscription to the <code>Publisher</code> as well
as any past subscription that is still currently active (for example, for long-running publishers
or even infinite publishers).</p>
</div>
</td>
</tr>
</table>
</div>
</div>
<div class="sect2">
<h3 id="scheduling-annotation-support-async"><a class="anchor" href="#scheduling-annotation-support-async"></a>The <code>@Async</code> annotation</h3>
<div class="paragraph">
<p>You can provide the <code>@Async</code> annotation on a method so that invocation of that method
occurs asynchronously. In other words, the caller returns immediately upon
invocation, while the actual execution of the method occurs in a task that has been
submitted to a Spring <code>TaskExecutor</code>. In the simplest case, you can apply the annotation
to a method that returns <code>void</code>, as the following example shows:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Async
void doSomething() {
	// this will be run asynchronously
}</code></pre>
</div>
</div>
<div class="paragraph">
<p>Unlike the methods annotated with the <code>@Scheduled</code> annotation, these methods can expect
arguments, because they are invoked in the &#8220;normal&#8221; way by callers at runtime rather
than from a scheduled task being managed by the container. For example, the following
code is a legitimate application of the <code>@Async</code> annotation:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Async
void doSomething(String s) {
	// this will be run asynchronously
}</code></pre>
</div>
</div>
<div class="paragraph">
<p>Even methods that return a value can be invoked asynchronously. However, such methods
are required to have a <code>Future</code>-typed return value. This still provides the benefit of
asynchronous execution so that the caller can perform other tasks prior to calling
<code>get()</code> on that <code>Future</code>. The following example shows how to use <code>@Async</code> on a method
that returns a value:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Async
Future&lt;String&gt; returnSomething(int i) {
	// this will be run asynchronously
}</code></pre>
</div>
</div>
<div class="admonitionblock tip">
<table>
<tr>
<td class="icon">
<i class="fa icon-tip" title="Tip"></i>
</td>
<td class="content">
<code>@Async</code> methods may not only declare a regular <code>java.util.concurrent.Future</code> return
type but also <code>java.util.concurrent.CompletableFuture</code>, for richer interaction with
the asynchronous task and for immediate composition with further processing steps.
</td>
</tr>
</table>
</div>
<div class="paragraph">
<p>You can not use <code>@Async</code> in conjunction with lifecycle callbacks such as <code>@PostConstruct</code>.
To asynchronously initialize Spring beans, you currently have to use a separate
initializing Spring bean that then invokes the <code>@Async</code> annotated method on the target,
as the following example shows:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">public class SampleBeanImpl implements SampleBean {

	@Async
	void doSomething() {
		// ...
	}

}

public class SampleBeanInitializer {

	private final SampleBean bean;

	public SampleBeanInitializer(SampleBean bean) {
		this.bean = bean;
	}

	@PostConstruct
	public void initialize() {
		bean.doSomething();
	}

}</code></pre>
</div>
</div>
<div class="admonitionblock note">
<table>
<tr>
<td class="icon">
<i class="fa icon-note" title="Note"></i>
</td>
<td class="content">
There is no direct XML equivalent for <code>@Async</code>, since such methods should be designed
for asynchronous execution in the first place, not externally re-declared to be asynchronous.
However, you can manually set up Spring&#8217;s <code>AsyncExecutionInterceptor</code> with Spring AOP,
in combination with a custom pointcut.
</td>
</tr>
</table>
</div>
</div>
<div class="sect2">
<h3 id="scheduling-annotation-support-qualification"><a class="anchor" href="#scheduling-annotation-support-qualification"></a>Executor Qualification with <code>@Async</code></h3>
<div class="paragraph">
<p>By default, when specifying <code>@Async</code> on a method, the executor that is used is the
one <a href="#scheduling-enable-annotation-support">configured when enabling async support</a>,
i.e. the &#8220;annotation-driven&#8221; element if you are using XML or your <code>AsyncConfigurer</code>
implementation, if any. However, you can use the <code>value</code> attribute of the <code>@Async</code>
annotation when you need to indicate that an executor other than the default should be
used when executing a given method. The following example shows how to do so:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Async("otherExecutor")
void doSomething(String s) {
	// this will be run asynchronously by "otherExecutor"
}</code></pre>
</div>
</div>
<div class="paragraph">
<p>In this case, <code>"otherExecutor"</code> can be the name of any <code>Executor</code> bean in the Spring
container, or it may be the name of a qualifier associated with any <code>Executor</code> (for example,
as specified with the <code>&lt;qualifier&gt;</code> element or Spring&#8217;s <code>@Qualifier</code> annotation).</p>
</div>
</div>
<div class="sect2">
<h3 id="scheduling-annotation-support-exception"><a class="anchor" href="#scheduling-annotation-support-exception"></a>Exception Management with <code>@Async</code></h3>
<div class="paragraph">
<p>When an <code>@Async</code> method has a <code>Future</code>-typed return value, it is easy to manage
an exception that was thrown during the method execution, as this exception is
thrown when calling <code>get</code> on the <code>Future</code> result. With a <code>void</code> return type,
however, the exception is uncaught and cannot be transmitted. You can provide an
<code>AsyncUncaughtExceptionHandler</code> to handle such exceptions. The following example shows
how to do so:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">public class MyAsyncUncaughtExceptionHandler implements AsyncUncaughtExceptionHandler {

	@Override
	public void handleUncaughtException(Throwable ex, Method method, Object... params) {
		// handle exception
	}
}</code></pre>
</div>
</div>
<div class="paragraph">
<p>By default, the exception is merely logged. You can define a custom <code>AsyncUncaughtExceptionHandler</code>
by using <code>AsyncConfigurer</code> or the <code>&lt;task:annotation-driven/&gt;</code> XML element.</p>
</div>
</div>
</div>
</div>
<div class="sect1">
<h2 id="scheduling-task-namespace"><a class="anchor" href="#scheduling-task-namespace"></a>The <code>task</code> Namespace</h2>
<div class="sectionbody">
<div class="paragraph">
<p>As of version 3.0, Spring includes an XML namespace for configuring <code>TaskExecutor</code> and
<code>TaskScheduler</code> instances. It also provides a convenient way to configure tasks to be
scheduled with a trigger.</p>
</div>
<div class="sect2">
<h3 id="scheduling-task-namespace-scheduler"><a class="anchor" href="#scheduling-task-namespace-scheduler"></a>The <code>scheduler</code> Element</h3>
<div class="paragraph">
<p>The following element creates a <code>ThreadPoolTaskScheduler</code> instance with the
specified thread pool size:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-xml hljs" data-lang="xml">&lt;task:scheduler id="scheduler" pool-size="10"/&gt;</code></pre>
</div>
</div>
<div class="paragraph">
<p>The value provided for the <code>id</code> attribute is used as the prefix for thread names
within the pool. The <code>scheduler</code> element is relatively straightforward. If you do not
provide a <code>pool-size</code> attribute, the default thread pool has only a single thread.
There are no other configuration options for the scheduler.</p>
</div>
</div>
<div class="sect2">
<h3 id="scheduling-task-namespace-executor"><a class="anchor" href="#scheduling-task-namespace-executor"></a>The <code>executor</code> Element</h3>
<div class="paragraph">
<p>The following creates a <code>ThreadPoolTaskExecutor</code> instance:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-xml hljs" data-lang="xml">&lt;task:executor id="executor" pool-size="10"/&gt;</code></pre>
</div>
</div>
<div class="paragraph">
<p>As with the scheduler shown in the <a href="#scheduling-task-namespace-scheduler">previous section</a>,
the value provided for the <code>id</code> attribute is used as the prefix for thread names within
the pool. As far as the pool size is concerned, the <code>executor</code> element supports more
configuration options than the <code>scheduler</code> element. For one thing, the thread pool for
a <code>ThreadPoolTaskExecutor</code> is itself more configurable. Rather than only a single size,
an executor&#8217;s thread pool can have different values for the core and the max size.
If you provide a single value, the executor has a fixed-size thread pool (the core and
max sizes are the same). However, the <code>executor</code> element&#8217;s <code>pool-size</code> attribute also
accepts a range in the form of <code>min-max</code>. The following example sets a minimum value of
<code>5</code> and a maximum value of <code>25</code>:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-xml hljs" data-lang="xml">&lt;task:executor
		id="executorWithPoolSizeRange"
		pool-size="5-25"
		queue-capacity="100"/&gt;</code></pre>
</div>
</div>
<div class="paragraph">
<p>In the preceding configuration, a <code>queue-capacity</code> value has also been provided.
The configuration of the thread pool should also be considered in light of the
executor&#8217;s queue capacity. For the full description of the relationship between pool
size and queue capacity, see the documentation for
<a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/ThreadPoolExecutor.html"><code>ThreadPoolExecutor</code></a>.
The main idea is that, when a task is submitted, the executor first tries to use a
free thread if the number of active threads is currently less than the core size.
If the core size has been reached, the task is added to the queue, as long as its
capacity has not yet been reached. Only then, if the queue&#8217;s capacity has been
reached, does the executor create a new thread beyond the core size. If the max size
has also been reached, then the executor rejects the task.</p>
</div>
<div class="paragraph">
<p>By default, the queue is unbounded, but this is rarely the desired configuration,
because it can lead to <code>OutOfMemoryError</code> if enough tasks are added to that queue while
all pool threads are busy. Furthermore, if the queue is unbounded, the max size has
no effect at all. Since the executor always tries the queue before creating a new
thread beyond the core size, a queue must have a finite capacity for the thread pool to
grow beyond the core size (this is why a fixed-size pool is the only sensible case
when using an unbounded queue).</p>
</div>
<div class="paragraph">
<p>Consider the case, as mentioned above, when a task is rejected. By default, when a
task is rejected, a thread pool executor throws a <code>TaskRejectedException</code>. However,
the rejection policy is actually configurable. The exception is thrown when using
the default rejection policy, which is the <code>AbortPolicy</code> implementation.
For applications where some tasks can be skipped under heavy load, you can instead
configure either <code>DiscardPolicy</code> or <code>DiscardOldestPolicy</code>. Another option that works
well for applications that need to throttle the submitted tasks under heavy load is
the <code>CallerRunsPolicy</code>. Instead of throwing an exception or discarding tasks,
that policy forces the thread that is calling the submit method to run the task itself.
The idea is that such a caller is busy while running that task and not able to submit
other tasks immediately. Therefore, it provides a simple way to throttle the incoming
load while maintaining the limits of the thread pool and queue. Typically, this allows
the executor to &#8220;catch up&#8221; on the tasks it is handling and thereby frees up some
capacity on the queue, in the pool, or both. You can choose any of these options from an
enumeration of values available for the <code>rejection-policy</code> attribute on the <code>executor</code>
element.</p>
</div>
<div class="paragraph">
<p>The following example shows an <code>executor</code> element with a number of attributes to specify
various behaviors:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-xml hljs" data-lang="xml">&lt;task:executor
		id="executorWithCallerRunsPolicy"
		pool-size="5-25"
		queue-capacity="100"
		rejection-policy="CALLER_RUNS"/&gt;</code></pre>
</div>
</div>
<div class="paragraph">
<p>Finally, the <code>keep-alive</code> setting determines the time limit (in seconds) for which threads
may remain idle before being stopped. If there are more than the core number of threads
currently in the pool, after waiting this amount of time without processing a task, excess
threads get stopped. A time value of zero causes excess threads to stop
immediately after executing a task without remaining follow-up work in the task queue.
The following example sets the <code>keep-alive</code> value to two minutes:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-xml hljs" data-lang="xml">&lt;task:executor
		id="executorWithKeepAlive"
		pool-size="5-25"
		keep-alive="120"/&gt;</code></pre>
</div>
</div>
</div>
<div class="sect2">
<h3 id="scheduling-task-namespace-scheduled-tasks"><a class="anchor" href="#scheduling-task-namespace-scheduled-tasks"></a>The <code>scheduled-tasks</code> Element</h3>
<div class="paragraph">
<p>The most powerful feature of Spring&#8217;s task namespace is the support for configuring
tasks to be scheduled within a Spring Application Context. This follows an approach
similar to other &#8220;method-invokers&#8221; in Spring, such as that provided by the JMS namespace
for configuring message-driven POJOs. Basically, a <code>ref</code> attribute can point to any
Spring-managed object, and the <code>method</code> attribute provides the name of a method to be
invoked on that object. The following listing shows a simple example:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-xml hljs" data-lang="xml">&lt;task:scheduled-tasks scheduler="myScheduler"&gt;
	&lt;task:scheduled ref="beanA" method="methodA" fixed-delay="5000"/&gt;
&lt;/task:scheduled-tasks&gt;

&lt;task:scheduler id="myScheduler" pool-size="10"/&gt;</code></pre>
</div>
</div>
<div class="paragraph">
<p>The scheduler is referenced by the outer element, and each individual
task includes the configuration of its trigger metadata. In the preceding example,
that metadata defines a periodic trigger with a fixed delay indicating the number of
milliseconds to wait after each task execution has completed. Another option is
<code>fixed-rate</code>, indicating how often the method should be run regardless of how long
any previous execution takes. Additionally, for both <code>fixed-delay</code> and <code>fixed-rate</code>
tasks, you can specify an 'initial-delay' parameter, indicating the number of
milliseconds to wait before the first execution of the method. For more control,
you can instead provide a <code>cron</code> attribute to provide a
<a href="#scheduling-cron-expression">cron expression</a>.
The following example shows these other options:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-xml hljs" data-lang="xml">&lt;task:scheduled-tasks scheduler="myScheduler"&gt;
	&lt;task:scheduled ref="beanA" method="methodA" fixed-delay="5000" initial-delay="1000"/&gt;
	&lt;task:scheduled ref="beanB" method="methodB" fixed-rate="5000"/&gt;
	&lt;task:scheduled ref="beanC" method="methodC" cron="*/5 * * * * MON-FRI"/&gt;
&lt;/task:scheduled-tasks&gt;

&lt;task:scheduler id="myScheduler" pool-size="10"/&gt;</code></pre>
</div>
</div>
</div>
</div>
</div>
<div class="sect1">
<h2 id="scheduling-cron-expression"><a class="anchor" href="#scheduling-cron-expression"></a>Cron Expressions</h2>
<div class="sectionbody">
<div class="paragraph">
<p>All Spring cron expressions have to conform to the same format, whether you are using them in
<a href="#scheduling-annotation-support-scheduled"><code>@Scheduled</code> annotations</a>,
<a href="#scheduling-task-namespace-scheduled-tasks"><code>task:scheduled-tasks</code> elements</a>,
or someplace else. A well-formed cron expression, such as <code>* * * * * *</code>, consists of six
space-separated time and date fields, each with its own range of valid values:</p>
</div>
<div class="literalblock">
<div class="content">
<pre> ┌───────────── second (0-59)
 │ ┌───────────── minute (0 - 59)
 │ │ ┌───────────── hour (0 - 23)
 │ │ │ ┌───────────── day of the month (1 - 31)
 │ │ │ │ ┌───────────── month (1 - 12) (or JAN-DEC)
 │ │ │ │ │ ┌───────────── day of the week (0 - 7)
 │ │ │ │ │ │          (0 or 7 is Sunday, or MON-SUN)
 │ │ │ │ │ │
 * * * * * *</pre>
</div>
</div>
<div class="paragraph">
<p>There are some rules that apply:</p>
</div>
<div class="ulist">
<ul>
<li>
<p>A field may be an asterisk (<code>*</code>), which always stands for &#8220;first-last&#8221;.
For the day-of-the-month or day-of-the-week fields, a question mark (<code>?</code>) may be used instead of an
asterisk.</p>
</li>
<li>
<p>Commas (<code>,</code>) are used to separate items of a list.</p>
</li>
<li>
<p>Two numbers separated with a hyphen (<code>-</code>) express a range of numbers.
The specified range is inclusive.</p>
</li>
<li>
<p>Following a range (or <code>*</code>) with <code>/</code> specifies the interval of the number&#8217;s value through the range.</p>
</li>
<li>
<p>English names can also be used for the month and day-of-week fields.
Use the first three letters of the particular day or month (case does not matter).</p>
</li>
<li>
<p>The day-of-month and day-of-week fields can contain an <code>L</code> character, which has a different meaning.</p>
<div class="ulist">
<ul>
<li>
<p>In the day-of-month field, <code>L</code> stands for <em>the last day of the month</em>.
If followed by a negative offset (that is, <code>L-n</code>), it means <em><code>n</code>th-to-last day of the month</em>.</p>
</li>
<li>
<p>In the day-of-week field, <code>L</code> stands for <em>the last day of the week</em>.
If prefixed by a number or three-letter name (<code>dL</code> or <code>DDDL</code>), it means <em>the last day of week (<code>d</code>
or <code>DDD</code>) in the month</em>.</p>
</li>
</ul>
</div>
</li>
<li>
<p>The day-of-month field can be <code>nW</code>, which stands for <em>the nearest weekday to day of the month <code>n</code></em>.
If <code>n</code> falls on Saturday, this yields the Friday before it.
If <code>n</code> falls on Sunday, this yields the Monday after, which also happens if <code>n</code> is <code>1</code> and falls on
a Saturday (that is: <code>1W</code> stands for <em>the first weekday of the month</em>).</p>
</li>
<li>
<p>If the day-of-month field is <code>LW</code>, it means <em>the last weekday of the month</em>.</p>
</li>
<li>
<p>The day-of-week field can be <code>d#n</code> (or <code>DDD#n</code>), which stands for <em>the <code>n</code>th day of week <code>d</code>
(or <code>DDD</code>) in the month</em>.</p>
</li>
</ul>
</div>
<div class="paragraph">
<p>Here are some examples:</p>
</div>
<table class="tableblock frame-all grid-all stripes-odd stretch">
<colgroup>
<col style="width: 50%;">
<col style="width: 50%;">
</colgroup>
<thead>
<tr>
<th class="tableblock halign-left valign-top">Cron Expression</th>
<th class="tableblock halign-left valign-top">Meaning</th>
</tr>
</thead>
<tbody>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>0 0 * * * *</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock">top of every hour of every day</p></td>
</tr>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>*/10 * * * * *</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock">every ten seconds</p></td>
</tr>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>0 0 8-10 * * *</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock">8, 9 and 10 o&#8217;clock of every day</p></td>
</tr>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>0 0 6,19 * * *</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock">6:00 AM and 7:00 PM every day</p></td>
</tr>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>0 0/30 8-10 * * *</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock">8:00, 8:30, 9:00, 9:30, 10:00 and 10:30 every day</p></td>
</tr>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>0 0 9-17 * * MON-FRI</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock">on the hour nine-to-five weekdays</p></td>
</tr>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>0 0 0 25 DEC ?</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock">every Christmas Day at midnight</p></td>
</tr>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>0 0 0 L * *</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock">last day of the month at midnight</p></td>
</tr>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>0 0 0 L-3 * *</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock">third-to-last day of the month at midnight</p></td>
</tr>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>0 0 0 * * 5L</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock">last Friday of the month at midnight</p></td>
</tr>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>0 0 0 * * THUL</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock">last Thursday of the month at midnight</p></td>
</tr>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>0 0 0 1W * *</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock">first weekday of the month at midnight</p></td>
</tr>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>0 0 0 LW * *</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock">last weekday of the month at midnight</p></td>
</tr>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>0 0 0 ? * 5#2</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock">the second Friday in the month at midnight</p></td>
</tr>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>0 0 0 ? * MON#1</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock">the first Monday in the month at midnight</p></td>
</tr>
</tbody>
</table>
<div class="sect2">
<h3 id="macros"><a class="anchor" href="#macros"></a>Macros</h3>
<div class="paragraph">
<p>Expressions such as <code>0 0 * * * *</code> are hard for humans to parse and are, therefore,
hard to fix in case of bugs. To improve readability, Spring supports the following
macros, which represent commonly used sequences. You can use these macros instead
of the six-digit value, thus: <code>@Scheduled(cron = "@hourly")</code>.</p>
</div>
<table class="tableblock frame-all grid-all stripes-odd stretch">
<colgroup>
<col style="width: 50%;">
<col style="width: 50%;">
</colgroup>
<thead>
<tr>
<th class="tableblock halign-left valign-top">Macro</th>
<th class="tableblock halign-left valign-top">Meaning</th>
</tr>
</thead>
<tbody>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>@yearly</code> (or <code>@annually</code>)</p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock">once a year (<code>0 0 0 1 1 *</code>)</p></td>
</tr>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>@monthly</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock">once a month (<code>0 0 0 1 * *</code>)</p></td>
</tr>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>@weekly</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock">once a week (<code>0 0 0 * * 0</code>)</p></td>
</tr>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>@daily</code> (or <code>@midnight</code>)</p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock">once a day (<code>0 0 0 * * *</code>), or</p></td>
</tr>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>@hourly</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock">once an hour, (<code>0 0 * * * *</code>)</p></td>
</tr>
</tbody>
</table>
</div>
</div>
</div>
<div class="sect1">
<h2 id="scheduling-quartz"><a class="anchor" href="#scheduling-quartz"></a>Using the Quartz Scheduler</h2>
<div class="sectionbody">
<div class="paragraph">
<p>Quartz uses <code>Trigger</code>, <code>Job</code>, and <code>JobDetail</code> objects to realize scheduling of all
kinds of jobs. For the basic concepts behind Quartz, see the
<a href="https://www.quartz-scheduler.org/">Quartz Web site</a>. For convenience purposes, Spring
offers a couple of classes that simplify using Quartz within Spring-based applications.</p>
</div>
<div class="sect2">
<h3 id="scheduling-quartz-jobdetail"><a class="anchor" href="#scheduling-quartz-jobdetail"></a>Using the <code>JobDetailFactoryBean</code></h3>
<div class="paragraph">
<p>Quartz <code>JobDetail</code> objects contain all the information needed to run a job. Spring
provides a <code>JobDetailFactoryBean</code>, which provides bean-style properties for XML
configuration purposes. Consider the following example:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-xml hljs" data-lang="xml">&lt;bean name="exampleJob" class="org.springframework.scheduling.quartz.JobDetailFactoryBean"&gt;
	&lt;property name="jobClass" value="example.ExampleJob"/&gt;
	&lt;property name="jobDataAsMap"&gt;
		&lt;map&gt;
			&lt;entry key="timeout" value="5"/&gt;
		&lt;/map&gt;
	&lt;/property&gt;
&lt;/bean&gt;</code></pre>
</div>
</div>
<div class="paragraph">
<p>The job detail configuration has all the information it needs to run the job (<code>ExampleJob</code>).
The timeout is specified in the job data map. The job data map is available through the
<code>JobExecutionContext</code> (passed to you at execution time), but the <code>JobDetail</code> also gets
its properties from the job data mapped to properties of the job instance. So, in the
following example, the <code>ExampleJob</code> contains a bean property named <code>timeout</code>, and the
<code>JobDetail</code> has it applied automatically:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">package example;

public class ExampleJob extends QuartzJobBean {

	private int timeout;

	/**
	 * Setter called after the ExampleJob is instantiated
	 * with the value from the JobDetailFactoryBean.
	 */
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	protected void executeInternal(JobExecutionContext ctx) throws JobExecutionException {
		// do the actual work
	}
}</code></pre>
</div>
</div>
<div class="paragraph">
<p>All additional properties from the job data map are available to you as well.</p>
</div>
<div class="admonitionblock note">
<table>
<tr>
<td class="icon">
<i class="fa icon-note" title="Note"></i>
</td>
<td class="content">
By using the <code>name</code> and <code>group</code> properties, you can modify the name and the group
of the job, respectively. By default, the name of the job matches the bean name
of the <code>JobDetailFactoryBean</code> (<code>exampleJob</code> in the preceding example above).
</td>
</tr>
</table>
</div>
</div>
<div class="sect2">
<h3 id="scheduling-quartz-method-invoking-job"><a class="anchor" href="#scheduling-quartz-method-invoking-job"></a>Using the <code>MethodInvokingJobDetailFactoryBean</code></h3>
<div class="paragraph">
<p>Often you merely need to invoke a method on a specific object. By using the
<code>MethodInvokingJobDetailFactoryBean</code>, you can do exactly this, as the following example shows:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-xml hljs" data-lang="xml">&lt;bean id="jobDetail" class="org.springframework.scheduling.quartz.MethodInvokingJobDetailFactoryBean"&gt;
	&lt;property name="targetObject" ref="exampleBusinessObject"/&gt;
	&lt;property name="targetMethod" value="doIt"/&gt;
&lt;/bean&gt;</code></pre>
</div>
</div>
<div class="paragraph">
<p>The preceding example results in the <code>doIt</code> method being called on the
<code>exampleBusinessObject</code> method, as the following example shows:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">public class ExampleBusinessObject {

	// properties and collaborators

	public void doIt() {
		// do the actual work
	}
}</code></pre>
</div>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-xml hljs" data-lang="xml">&lt;bean id="exampleBusinessObject" class="examples.ExampleBusinessObject"/&gt;</code></pre>
</div>
</div>
<div class="paragraph">
<p>By using the <code>MethodInvokingJobDetailFactoryBean</code>, you need not create one-line jobs
that merely invoke a method. You need only create the actual business object and
wire up the detail object.</p>
</div>
<div class="paragraph">
<p>By default, Quartz Jobs are stateless, resulting in the possibility of jobs interfering
with each other. If you specify two triggers for the same <code>JobDetail</code>, it is possible
that the second one starts before the first job has finished. If <code>JobDetail</code> classes
implement the <code>Stateful</code> interface, this does not happen: the second job does not start
before the first one has finished.</p>
</div>
<div class="paragraph">
<p>To make jobs resulting from the <code>MethodInvokingJobDetailFactoryBean</code> be non-concurrent,
set the <code>concurrent</code> flag to <code>false</code>, as the following example shows:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-xml hljs" data-lang="xml">&lt;bean id="jobDetail" class="org.springframework.scheduling.quartz.MethodInvokingJobDetailFactoryBean"&gt;
	&lt;property name="targetObject" ref="exampleBusinessObject"/&gt;
	&lt;property name="targetMethod" value="doIt"/&gt;
	&lt;property name="concurrent" value="false"/&gt;
&lt;/bean&gt;</code></pre>
</div>
</div>
<div class="admonitionblock note">
<table>
<tr>
<td class="icon">
<i class="fa icon-note" title="Note"></i>
</td>
<td class="content">
By default, jobs will run in a concurrent fashion.
</td>
</tr>
</table>
</div>
</div>
<div class="sect2">
<h3 id="scheduling-quartz-cron"><a class="anchor" href="#scheduling-quartz-cron"></a>Wiring up Jobs by Using Triggers and <code>SchedulerFactoryBean</code></h3>
<div class="paragraph">
<p>We have created job details and jobs. We have also reviewed the convenience bean that
lets you invoke a method on a specific object. Of course, we still need to schedule the
jobs themselves. This is done by using triggers and a <code>SchedulerFactoryBean</code>. Several
triggers are available within Quartz, and Spring offers two Quartz <code>FactoryBean</code>
implementations with convenient defaults: <code>CronTriggerFactoryBean</code> and
<code>SimpleTriggerFactoryBean</code>.</p>
</div>
<div class="paragraph">
<p>Triggers need to be scheduled. Spring offers a <code>SchedulerFactoryBean</code> that exposes
triggers to be set as properties. <code>SchedulerFactoryBean</code> schedules the actual jobs with
those triggers.</p>
</div>
<div class="paragraph">
<p>The following listing uses both a <code>SimpleTriggerFactoryBean</code> and a <code>CronTriggerFactoryBean</code>:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-xml hljs" data-lang="xml">&lt;bean id="simpleTrigger" class="org.springframework.scheduling.quartz.SimpleTriggerFactoryBean"&gt;
	&lt;!-- see the example of method invoking job above --&gt;
	&lt;property name="jobDetail" ref="jobDetail"/&gt;
	&lt;!-- 10 seconds --&gt;
	&lt;property name="startDelay" value="10000"/&gt;
	&lt;!-- repeat every 50 seconds --&gt;
	&lt;property name="repeatInterval" value="50000"/&gt;
&lt;/bean&gt;

&lt;bean id="cronTrigger" class="org.springframework.scheduling.quartz.CronTriggerFactoryBean"&gt;
	&lt;property name="jobDetail" ref="exampleJob"/&gt;
	&lt;!-- run every morning at 6 AM --&gt;
	&lt;property name="cronExpression" value="0 0 6 * * ?"/&gt;
&lt;/bean&gt;</code></pre>
</div>
</div>
<div class="paragraph">
<p>The preceding example sets up two triggers, one running every 50 seconds with a starting
delay of 10 seconds and one running every morning at 6 AM. To finalize everything,
we need to set up the <code>SchedulerFactoryBean</code>, as the following example shows:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-xml hljs" data-lang="xml">&lt;bean class="org.springframework.scheduling.quartz.SchedulerFactoryBean"&gt;
	&lt;property name="triggers"&gt;
		&lt;list&gt;
			&lt;ref bean="cronTrigger"/&gt;
			&lt;ref bean="simpleTrigger"/&gt;
		&lt;/list&gt;
	&lt;/property&gt;
&lt;/bean&gt;</code></pre>
</div>
</div>
<div class="paragraph">
<p>More properties are available for the <code>SchedulerFactoryBean</code>, such as the calendars used by the
job details, properties to customize Quartz with, and a Spring-provided JDBC DataSource. See
the <a href="https://docs.spring.io/spring-framework/docs/7.0.9/javadoc-api/org/springframework/scheduling/quartz/SchedulerFactoryBean.html"><code>SchedulerFactoryBean</code></a>
javadoc for more information.</p>
</div>
<div class="admonitionblock note">
<table>
<tr>
<td class="icon">
<i class="fa icon-note" title="Note"></i>
</td>
<td class="content">
<code>SchedulerFactoryBean</code> also recognizes a <code>quartz.properties</code> file in the classpath,
based on Quartz property keys, as with regular Quartz configuration. Please note that many
<code>SchedulerFactoryBean</code> settings interact with common Quartz settings in the properties file;
it is therefore not recommended to specify values at both levels. For example, do not set
an "org.quartz.jobStore.class" property if you mean to rely on a Spring-provided DataSource,
or specify an <code>org.springframework.scheduling.quartz.LocalDataSourceJobStore</code> variant which
is a full-fledged replacement for the standard <code>org.quartz.impl.jdbcjobstore.JobStoreTX</code>.
</td>
</tr>
</table>
</div>
</div>
</div>
</div>
<nav class="pagination">
  <span class="prev"><a href="email.html">Email</a></span>
  <span class="next"><a href="cache.html">Cache Abstraction</a></span>
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
                          <a class="title" href="../index.html">Spring Framework</a>
                        </div>                        <div class="version-item is-active">
                          <div>
                            <button class="version-toggle" type="button">
                              <span></span>
                              Stable
                            </button>
                          </div>
                          <ul class="versions">
  <li class="version">
    <a href="scheduling.html">
      7.0.9
    </a>
  </li>
  <li class="version">
    <a href="../6.2/integration/scheduling.html">
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
    <a href="../7.1/integration/scheduling.html">
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
    <a href="../7.1-SNAPSHOT/integration/scheduling.html">
      7.1.0-SNAPSHOT
    </a>
  </li>
  <li class="version">
    <a href="../7.0-SNAPSHOT/integration/scheduling.html">
      7.0.10-SNAPSHOT
    </a>
  </li>
  <li class="version">
    <a href="../6.2-SNAPSHOT/integration/scheduling.html">
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
<script async id="search-script" src="../_/js/vendor/search.js" data-app-id="WB1FQYI187" data-api-key="c2e84f15fa630d534f1c62b1c413bb77" data-index-name="springdocs" data-stylesheet="../_/css/vendor/search.css" data-page-version="7.0.9" data-page-generation="7.0" data-page-component="framework"></script>
  </body>
</html>

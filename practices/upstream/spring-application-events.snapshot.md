<!DOCTYPE html>
<html lang="en">
  <script src="https://cdn.cookielaw.org/scripttemplates/otSDKStub.js" data-domain-script="018ee325-b3a7-7753-937b-b8b3e643b1a7"></script><script>function OptanonWrapper() {}</script><script>function setGTM(w, d, s, l, i) { w[l] = w[l] || []; w[l].push({ "gtm.start": new Date().getTime(), event: "gtm.js"}); var f = d.getElementsByTagName(s)[0], j = d.createElement(s), dl = l != "dataLayer" ? "&l=" + l : ""; j.async = true; j.src = "https://www.googletagmanager.com/gtm.js?id=" + i + dl; f.parentNode.insertBefore(j, f); } if (document.cookie.indexOf("OptanonConsent") > -1 && document.cookie.indexOf("groups=") > -1) { setGTM(window, document, "script", "dataLayer", "GTM-W8CQ8TL"); } else { waitForOnetrustActiveGroups(); } var timer; function waitForOnetrustActiveGroups() { if (document.cookie.indexOf("OptanonConsent") > -1 && document.cookie.indexOf("groups=") > -1) { clearTimeout(timer); setGTM(window, document, "script", "dataLayer", "GTM-W8CQ8TL"); } else { timer = setTimeout(waitForOnetrustActiveGroups, 250); }}</script>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>Additional Capabilities of the ApplicationContext :: Spring Framework</title>
    <link rel="canonical" href="https://docs.spring.io/spring-framework/reference/core/beans/context-introduction.html">
    <link rel="prev" href="context-load-time-weaver.html">
    <link rel="next" href="beanfactory.html">
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
    <meta name="versioned-url" content="https://docs.spring.io/spring-framework/reference/7.0/core/beans/context-introduction.html">
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
    <a class="nav-link"  href="../beans.html">The IoC Container</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="introduction.html">Introduction to the Spring IoC Container and Beans</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="basics.html">Container Overview</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="definition.html">Bean Overview</a>
  </li>
  <li class="nav-item" data-depth="3">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="dependencies.html">Dependencies</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="dependencies/factory-collaborators.html">Dependency Injection</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="dependencies/factory-properties-detailed.html">Dependencies and Configuration in Detail</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="dependencies/factory-dependson.html">Using <code>depends-on</code></a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="dependencies/factory-lazy-init.html">Lazy-initialized Beans</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="dependencies/factory-autowire.html">Autowiring Collaborators</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="dependencies/factory-method-injection.html">Method Injection</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="factory-scopes.html">Bean Scopes</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="factory-nature.html">Customizing the Nature of a Bean</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="child-bean-definitions.html">Bean Definition Inheritance</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="factory-extension.html">Container Extension Points</a>
  </li>
  <li class="nav-item" data-depth="3">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="annotation-config.html">Annotation-based Container Configuration</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="annotation-config/autowired.html">Using <code>@Autowired</code></a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="annotation-config/autowired-primary.html">Fine-tuning Annotation-based Autowiring with <code>@Primary</code> or <code>@Fallback</code></a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="annotation-config/autowired-qualifiers.html">Fine-tuning Annotation-based Autowiring with Qualifiers</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="annotation-config/generics-as-qualifiers.html">Using Generics as Autowiring Qualifiers</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="annotation-config/custom-autowire-configurer.html">Using <code>CustomAutowireConfigurer</code></a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="annotation-config/resource.html">Injection with <code>@Resource</code></a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="annotation-config/value-annotations.html">Using <code>@Value</code></a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="annotation-config/postconstruct-and-predestroy-annotations.html">Using <code>@PostConstruct</code> and <code>@PreDestroy</code></a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="classpath-scanning.html">Classpath Scanning and Managed Components</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="standard-annotations.html">Using JSR-330 Standard Annotations</a>
  </li>
  <li class="nav-item" data-depth="3">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="java.html">Java-based Container Configuration</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="java/basic-concepts.html">Basic Concepts: <code>@Bean</code> and <code>@Configuration</code></a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="java/instantiating-container.html">Instantiating the Spring Container by Using <code>AnnotationConfigApplicationContext</code></a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="java/bean-annotation.html">Using the <code>@Bean</code> Annotation</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="java/configuration-annotation.html">Using the <code>@Configuration</code> annotation</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="java/composing-configuration-classes.html">Composing Java-based Configurations</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="java/programmatic-bean-registration.html">Programmatic Bean Registration</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="environment.html">Environment Abstraction</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="context-load-time-weaver.html">Registering a <code>LoadTimeWeaver</code></a>
  </li>
  <li class="nav-item is-current-page" data-depth="3">
    <a class="nav-link"  href="context-introduction.html">Additional Capabilities of the <code>ApplicationContext</code></a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="beanfactory.html">The <code>BeanFactory</code> API</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../resources.html">Resources</a>
  </li>
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../validation.html">Validation, Data Binding, and Type Conversion</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../validation/validator.html">Validation Using Spring&#8217;s Validator Interface</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../validation/data-binding.html">Data Binding</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../validation/error-code-resolution.html">Resolving Error Codes to Error Messages</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../validation/convert.html">Spring Type Conversion</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../validation/format.html">Spring Field Formatting</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../validation/format-configuring-formatting-globaldatetimeformat.html">Configuring a Global Date and Time Format</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../validation/beanvalidation.html">Java Bean Validation</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../expressions.html">Spring Expression Language (SpEL)</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../expressions/evaluation.html">Evaluation</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../expressions/beandef.html">Expressions in Bean Definitions</a>
  </li>
  <li class="nav-item" data-depth="3">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../expressions/language-ref.html">Language Reference</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../expressions/language-ref/literal.html">Literal Expressions</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../expressions/language-ref/properties-arrays.html">Properties, Arrays, Lists, Maps, and Indexers</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../expressions/language-ref/inline-lists.html">Inline Lists</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../expressions/language-ref/inline-maps.html">Inline Maps</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../expressions/language-ref/array-construction.html">Array Construction</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../expressions/language-ref/methods.html">Methods</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../expressions/language-ref/operators.html">Operators</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../expressions/language-ref/types.html">Types</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../expressions/language-ref/constructors.html">Constructors</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../expressions/language-ref/variables.html">Variables</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../expressions/language-ref/functions.html">Functions</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../expressions/language-ref/varargs.html">Varargs Invocations</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../expressions/language-ref/bean-references.html">Bean References</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../expressions/language-ref/operator-ternary.html">Ternary Operator (If-Then-Else)</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../expressions/language-ref/operator-elvis.html">The Elvis Operator</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../expressions/language-ref/operator-safe-navigation.html">Safe Navigation Operator</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../expressions/language-ref/collection-selection.html">Collection Selection</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../expressions/language-ref/collection-projection.html">Collection Projection</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../expressions/language-ref/templating.html">Expression Templating</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../expressions/example-classes.html">Classes Used in the Examples</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../aop.html">Aspect Oriented Programming with Spring</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../aop/introduction-defn.html">AOP Concepts</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../aop/introduction-spring-defn.html">Spring AOP Capabilities and Goals</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../aop/introduction-proxies.html">AOP Proxies</a>
  </li>
  <li class="nav-item" data-depth="3">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../aop/ataspectj.html">@AspectJ support</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../aop/ataspectj/aspectj-support.html">Enabling @AspectJ Support</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../aop/ataspectj/at-aspectj.html">Declaring an Aspect</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../aop/ataspectj/pointcuts.html">Declaring a Pointcut</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../aop/ataspectj/advice.html">Declaring Advice</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../aop/ataspectj/introductions.html">Introductions</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../aop/ataspectj/instantiation-models.html">Aspect Instantiation Models</a>
  </li>
  <li class="nav-item" data-depth="4">
    <a class="nav-link"  href="../aop/ataspectj/example.html">An AOP Example</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../aop/schema.html">Schema-based AOP Support</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../aop/choosing.html">Choosing which AOP Declaration Style to Use</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../aop/mixing-styles.html">Mixing Aspect Types</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../aop/proxying.html">Proxying Mechanisms</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../aop/aspectj-programmatic.html">Programmatic Creation of @AspectJ Proxies</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../aop/using-aspectj.html">Using AspectJ with Spring Applications</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../aop/resources.html">Further Resources</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../aop-api.html">Spring AOP APIs</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../aop-api/pointcuts.html">Pointcut API in Spring</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../aop-api/advice.html">Advice API in Spring</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../aop-api/advisor.html">The Advisor API in Spring</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../aop-api/pfb.html">Using the <code>ProxyFactoryBean</code> to Create AOP Proxies</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../aop-api/concise-proxy.html">Concise Proxy Definitions</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../aop-api/prog.html">Creating AOP Proxies Programmatically with the <code>ProxyFactory</code></a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../aop-api/advised.html">Manipulating Advised Objects</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../aop-api/autoproxy.html">Using the "auto-proxy" facility</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../aop-api/targetsource.html">Using <code>TargetSource</code> Implementations</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../aop-api/extensibility.html">Defining New Advice Types</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../resilience.html">Resilience Features</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../null-safety.html">Null-safety</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../databuffer-codec.html">Data Buffers and Codecs</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../aot.html">Ahead of Time Optimizations</a>
  </li>
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../appendix.html">Appendix</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../appendix/xsd-schemas.html">XML Schemas</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../appendix/xml-custom.html">XML Schema Authoring</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../appendix/application-startup-steps.html">Application Startup Steps</a>
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
    <a class="nav-link"  href="../../integration/rest-clients.html">REST Clients</a>
  </li>
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../../integration/jms.html">JMS (Java Message Service)</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../integration/jms/using.html">Using Spring JMS</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../integration/jms/sending.html">Sending a Message</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../integration/jms/receiving.html">Receiving a Message</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../integration/jms/jca-message-endpoint-manager.html">Support for JCA Message Endpoints</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../integration/jms/annotated.html">Annotation-driven Listener Endpoints</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../integration/jms/namespace.html">JMS Namespace Support</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../../integration/jmx.html">JMX</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../integration/jmx/exporting.html">Exporting Your Beans to JMX</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../integration/jmx/interface.html">Controlling the Management Interface of Your Beans</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../integration/jmx/naming.html">Controlling  <code>ObjectName</code> Instances for Your Beans</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../integration/jmx/jsr160.html">Using JSR-160 Connectors</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../integration/jmx/proxy.html">Accessing MBeans through Proxies</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../integration/jmx/notifications.html">Notifications</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../integration/jmx/resources.html">Further Resources</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../../integration/email.html">Email</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../../integration/scheduling.html">Task Execution and Scheduling</a>
  </li>
  <li class="nav-item" data-depth="2">
    <button class="nav-item-toggle"></button>
    <a class="nav-link"  href="../../integration/cache.html">Cache Abstraction</a>
<ul class="nav-list">
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../integration/cache/strategies.html">Understanding the Cache Abstraction</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../integration/cache/annotations.html">Declarative Annotation-based Caching</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../integration/cache/jsr-107.html">JCache (JSR-107) Annotations</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../integration/cache/declarative-xml.html">Declarative XML-based Caching</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../integration/cache/store-configuration.html">Configuring the Cache Storage</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../integration/cache/plug.html">Plugging-in Different Back-end Caches</a>
  </li>
  <li class="nav-item" data-depth="3">
    <a class="nav-link"  href="../../integration/cache/specific-config.html">How can I Set the TTL/TTI/Eviction policy/XXX feature?</a>
  </li>
</ul>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../../integration/observability.html">Observability Support</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../../integration/aot-cache.html">JVM AOT Cache</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../../integration/checkpoint-restore.html">JVM Checkpoint Restore</a>
  </li>
  <li class="nav-item" data-depth="2">
    <a class="nav-link"  href="../../integration/appendix.html">Appendix</a>
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
      data-title="Additional Capabilities of the ApplicationContext"
      data-levels="2"
    >
      <div class="toc-menu"></div>
    </div>
    <div class="sidebar-links">
        <a href="https://github.com/spring-projects/spring-framework/blob/v7.0.9/framework-docs/modules/ROOT/pages/core/beans/context-introduction.adoc">
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
      <li><a href="../../core.html">Core Technologies</a></li>
      <li><a href="../beans.html">The IoC Container</a></li>
      <li><a href="context-introduction.html">Additional Capabilities of the <code>ApplicationContext</code></a></li>
    </ul>
  </nav>
</div><h1 id="page-title" class="page">Additional Capabilities of the <code>ApplicationContext</code></h1>
<div id="preamble">
<div class="sectionbody">
<div class="paragraph">
<p>As discussed in the <a href="introduction.html" class="xref page">chapter introduction</a>, the <code>org.springframework.beans.factory</code>
package provides basic functionality for managing and manipulating beans, including in a
programmatic way. The <code>org.springframework.context</code> package adds the
<a href="https://docs.spring.io/spring-framework/docs/7.0.9/javadoc-api/org/springframework/context/ApplicationContext.html"><code>ApplicationContext</code></a>
interface, which extends the <code>BeanFactory</code> interface, in addition to extending other
interfaces to provide additional functionality in a more application
framework-oriented style. Many people use the <code>ApplicationContext</code> in a completely
declarative fashion, not even creating it programmatically, but instead relying on
support classes such as <code>ContextLoader</code> to automatically instantiate an
<code>ApplicationContext</code> as part of the normal startup process of a Jakarta EE web application.</p>
</div>
<div class="paragraph">
<p>To enhance <code>BeanFactory</code> functionality in a more framework-oriented style, the context
package also provides the following functionality:</p>
</div>
<div class="ulist">
<ul>
<li>
<p>Access to messages in i18n-style, through the <code>MessageSource</code> interface.</p>
</li>
<li>
<p>Access to resources, such as URLs and files, through the <code>ResourceLoader</code> interface.</p>
</li>
<li>
<p>Event publication, namely to beans that implement the <code>ApplicationListener</code> interface,
through the use of the <code>ApplicationEventPublisher</code> interface.</p>
</li>
<li>
<p>Loading of multiple (hierarchical) contexts, letting each be focused on one
particular layer, such as the web layer of an application, through the
<code>HierarchicalBeanFactory</code> interface.</p>
</li>
</ul>
</div>
</div>
</div>
<div class="sect1">
<h2 id="context-functionality-messagesource"><a class="anchor" href="#context-functionality-messagesource"></a>Internationalization using <code>MessageSource</code></h2>
<div class="sectionbody">
<div class="paragraph">
<p>The <code>ApplicationContext</code> interface extends an interface called <code>MessageSource</code> and,
therefore, provides internationalization (&#8220;i18n&#8221;) functionality. Spring also provides the
<code>HierarchicalMessageSource</code> interface, which can resolve messages hierarchically.
Together, these interfaces provide the foundation upon which Spring effects message
resolution. The methods defined on these interfaces include:</p>
</div>
<div class="ulist">
<ul>
<li>
<p><code>String getMessage(String code, Object[] args, String default, Locale loc)</code>: The basic
method used to retrieve a message from the <code>MessageSource</code>. When no message is found
for the specified locale, the default message is used. Any arguments passed in become
replacement values, using the <code>MessageFormat</code> functionality provided by the standard
library.</p>
</li>
<li>
<p><code>String getMessage(String code, Object[] args, Locale loc)</code>: Essentially the same as
the previous method but with one difference: No default message can be specified. If
the message cannot be found, a <code>NoSuchMessageException</code> is thrown.</p>
</li>
<li>
<p><code>String getMessage(MessageSourceResolvable resolvable, Locale locale)</code>: All properties
used in the preceding methods are also wrapped in a class named
<code>MessageSourceResolvable</code>, which you can use with this method.</p>
</li>
</ul>
</div>
<div class="paragraph">
<p>When an <code>ApplicationContext</code> is loaded, it automatically searches for a <code>MessageSource</code>
bean defined in the context. The bean must have the name <code>messageSource</code>. If such a bean
is found, all calls to the preceding methods are delegated to the message source. If no
message source is found, the <code>ApplicationContext</code> attempts to find a parent containing a
bean with the same name. If it does, it uses that bean as the <code>MessageSource</code>. If the
<code>ApplicationContext</code> cannot find any source for messages, an empty
<code>DelegatingMessageSource</code> is instantiated in order to be able to accept calls to the
methods defined above.</p>
</div>
<div class="paragraph">
<p>Spring provides three <code>MessageSource</code> implementations, <code>ResourceBundleMessageSource</code>, <code>ReloadableResourceBundleMessageSource</code>
and <code>StaticMessageSource</code>. All of them implement <code>HierarchicalMessageSource</code> in order to do nested
messaging. The <code>StaticMessageSource</code> is rarely used but provides programmatic ways to
add messages to the source. The following example shows <code>ResourceBundleMessageSource</code>:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-xml hljs" data-lang="xml">&lt;beans&gt;
	&lt;bean id="messageSource"
			class="org.springframework.context.support.ResourceBundleMessageSource"&gt;
		&lt;property name="basenames"&gt;
			&lt;list&gt;
				&lt;value&gt;format&lt;/value&gt;
				&lt;value&gt;exceptions&lt;/value&gt;
				&lt;value&gt;windows&lt;/value&gt;
			&lt;/list&gt;
		&lt;/property&gt;
	&lt;/bean&gt;
&lt;/beans&gt;</code></pre>
</div>
</div>
<div class="paragraph">
<p>The example assumes that you have three resource bundles called <code>format</code>, <code>exceptions</code> and <code>windows</code>
defined in your classpath. Any request to resolve a message is
handled in the JDK-standard way of resolving messages through <code>ResourceBundle</code> objects. For the
purposes of the example, assume the contents of two of the above resource bundle files
are as follows:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-properties hljs" data-lang="properties"># in format.properties
message=Alligators rock!</code></pre>
</div>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-properties hljs" data-lang="properties"># in exceptions.properties
argument.required=The {0} argument is required.</code></pre>
</div>
</div>
<div class="paragraph">
<p>The next example shows a program to run the <code>MessageSource</code> functionality.
Remember that all <code>ApplicationContext</code> implementations are also <code>MessageSource</code>
implementations and so can be cast to the <code>MessageSource</code> interface.</p>
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
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">public static void main(String[] args) {
	MessageSource resources = new ClassPathXmlApplicationContext("beans.xml");
	String message = resources.getMessage("message", null, "Default", Locale.ENGLISH);
	System.out.println(message);
}</code></pre>
</div>
</div>
</div>
<div id="_tabs_1_kotlin--panel" class="tabpanel" aria-labelledby="_tabs_1_kotlin">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-kotlin hljs" data-lang="kotlin">fun main() {
	val resources = ClassPathXmlApplicationContext("beans.xml")
	val message = resources.getMessage("message", null, "Default", Locale.ENGLISH)
	println(message)
}</code></pre>
</div>
</div>
</div>
</div>
</div>
<div class="paragraph">
<p>The resulting output from the above program is as follows:</p>
</div>
<div class="literalblock">
<div class="content">
<pre>Alligators rock!</pre>
</div>
</div>
<div class="paragraph">
<p>To summarize, the <code>MessageSource</code> is defined in a file called <code>beans.xml</code>, which
exists at the root of your classpath. The <code>messageSource</code> bean definition refers to a
number of resource bundles through its <code>basenames</code> property. The three files that are
passed in the list to the <code>basenames</code> property exist as files at the root of your
classpath and are called <code>format.properties</code>, <code>exceptions.properties</code>, and
<code>windows.properties</code>, respectively.</p>
</div>
<div class="paragraph">
<p>The next example shows arguments passed to the message lookup. These arguments are
converted into <code>String</code> objects and inserted into placeholders in the lookup message.</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-xml hljs" data-lang="xml">&lt;beans&gt;

	&lt;!-- this MessageSource is being used in a web application --&gt;
	&lt;bean id="messageSource" class="org.springframework.context.support.ResourceBundleMessageSource"&gt;
		&lt;property name="basename" value="exceptions"/&gt;
	&lt;/bean&gt;

	&lt;!-- lets inject the above MessageSource into this POJO --&gt;
	&lt;bean id="example" class="com.something.Example"&gt;
		&lt;property name="messages" ref="messageSource"/&gt;
	&lt;/bean&gt;

&lt;/beans&gt;</code></pre>
</div>
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
</ul>
</div>
<div id="_tabs_2_java--panel" class="tabpanel" aria-labelledby="_tabs_2_java">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">public class Example {

	private MessageSource messages;

	public void setMessages(MessageSource messages) {
		this.messages = messages;
	}

	public void execute() {
		String message = this.messages.getMessage("argument.required",
			new Object [] {"userDao"}, "Required", Locale.ENGLISH);
		System.out.println(message);
	}
}</code></pre>
</div>
</div>
</div>
<div id="_tabs_2_kotlin--panel" class="tabpanel" aria-labelledby="_tabs_2_kotlin">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-kotlin hljs" data-lang="kotlin">	class Example {

	lateinit var messages: MessageSource

	fun execute() {
		val message = messages.getMessage("argument.required",
				arrayOf("userDao"), "Required", Locale.ENGLISH)
		println(message)
	}
}</code></pre>
</div>
</div>
</div>
</div>
</div>
<div class="paragraph">
<p>The resulting output from the invocation of the <code>execute()</code> method is as follows:</p>
</div>
<div class="literalblock">
<div class="content">
<pre>The userDao argument is required.</pre>
</div>
</div>
<div class="paragraph">
<p>With regard to internationalization (&#8220;i18n&#8221;), Spring&#8217;s various <code>MessageSource</code>
implementations follow the same locale resolution and fallback rules as the standard JDK
<code>ResourceBundle</code>. In short, and continuing with the example <code>messageSource</code> defined
previously, if you want to resolve messages against the British (<code>en-GB</code>) locale, you
would create files called <code>format_en_GB.properties</code>, <code>exceptions_en_GB.properties</code>, and
<code>windows_en_GB.properties</code>, respectively.</p>
</div>
<div class="paragraph">
<p>Typically, locale resolution is managed by the surrounding environment of the
application. In the following example, the locale against which (British) messages are
resolved is specified manually:</p>
</div>
<div class="literalblock">
<div class="content">
<pre># in exceptions_en_GB.properties
argument.required=Ebagum lad, the ''{0}'' argument is required, I say, required.</pre>
</div>
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
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">public static void main(final String[] args) {
	MessageSource resources = new ClassPathXmlApplicationContext("beans.xml");
	String message = resources.getMessage("argument.required",
		new Object [] {"userDao"}, "Required", Locale.UK);
	System.out.println(message);
}</code></pre>
</div>
</div>
</div>
<div id="_tabs_3_kotlin--panel" class="tabpanel" aria-labelledby="_tabs_3_kotlin">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-kotlin hljs" data-lang="kotlin">fun main() {
	val resources = ClassPathXmlApplicationContext("beans.xml")
	val message = resources.getMessage("argument.required",
			arrayOf("userDao"), "Required", Locale.UK)
	println(message)
}</code></pre>
</div>
</div>
</div>
</div>
</div>
<div class="paragraph">
<p>The resulting output from the running of the above program is as follows:</p>
</div>
<div class="literalblock">
<div class="content">
<pre>Ebagum lad, the 'userDao' argument is required, I say, required.</pre>
</div>
</div>
<div class="paragraph">
<p>You can also use the <code>MessageSourceAware</code> interface to acquire a reference to any
<code>MessageSource</code> that has been defined. Any bean that is defined in an
<code>ApplicationContext</code> that implements the <code>MessageSourceAware</code> interface is injected with
the application context&#8217;s <code>MessageSource</code> when the bean is created and configured.</p>
</div>
<div class="admonitionblock note">
<table>
<tr>
<td class="icon">
<i class="fa icon-note" title="Note"></i>
</td>
<td class="content">
Because Spring&#8217;s <code>MessageSource</code> is based on Java&#8217;s <code>ResourceBundle</code>, it does not merge
bundles with the same base name, but will only use the first bundle found.
Subsequent message bundles with the same base name are ignored.
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
As an alternative to <code>ResourceBundleMessageSource</code>, Spring provides a
<code>ReloadableResourceBundleMessageSource</code> class. This variant supports the same bundle
file format but is more flexible than the standard JDK based
<code>ResourceBundleMessageSource</code> implementation. In particular, it allows for reading
files from any Spring resource location (not only from the classpath) and supports hot
reloading of bundle property files (while efficiently caching them in between).
See the <a href="https://docs.spring.io/spring-framework/docs/7.0.9/javadoc-api/org/springframework/context/support/ReloadableResourceBundleMessageSource.html"><code>ReloadableResourceBundleMessageSource</code></a>
javadoc for details.
</td>
</tr>
</table>
</div>
</div>
</div>
<div class="sect1">
<h2 id="context-functionality-events"><a class="anchor" href="#context-functionality-events"></a>Standard and Custom Events</h2>
<div class="sectionbody">
<div class="paragraph">
<p>Event handling in the <code>ApplicationContext</code> is provided through the <code>ApplicationEvent</code>
class and the <code>ApplicationListener</code> interface. If a bean that implements the
<code>ApplicationListener</code> interface is deployed into the context, every time an
<code>ApplicationEvent</code> gets published to the <code>ApplicationContext</code>, that bean is notified.
Essentially, this is the standard Observer design pattern.</p>
</div>
<div class="admonitionblock tip">
<table>
<tr>
<td class="icon">
<i class="fa icon-tip" title="Tip"></i>
</td>
<td class="content">
As of Spring 4.2, the event infrastructure has been significantly improved and offers
an <a href="#context-functionality-events-annotation">annotation-based model</a> as well as the
ability to publish any arbitrary event (that is, an object that does not necessarily
extend from <code>ApplicationEvent</code>). When such an object is published, we wrap it in an
event for you.
</td>
</tr>
</table>
</div>
<div class="paragraph">
<p>The following table describes the standard events that Spring provides:</p>
</div>
<table id="beans-ctx-events-tbl" class="tableblock frame-all grid-all stripes-odd stretch">
<caption class="title">Table 1. Built-in Events</caption>
<colgroup>
<col style="width: 30%;">
<col style="width: 70%;">
</colgroup>
<thead>
<tr>
<th class="tableblock halign-left valign-top">Event</th>
<th class="tableblock halign-left valign-top">Explanation</th>
</tr>
</thead>
<tbody>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>ContextRefreshedEvent</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock">Published when the <code>ApplicationContext</code> is initialized or refreshed (for example, by
  using the <code>refresh()</code> method on the <code>ConfigurableApplicationContext</code> interface).
  Here, &#8220;initialized&#8221; means that all beans are loaded, post-processor beans are detected
  and activated, singletons are pre-instantiated, and the <code>ApplicationContext</code> object is
  ready for use. As long as the context has not been closed, a refresh can be triggered
  multiple times, provided that the chosen <code>ApplicationContext</code> actually supports such
  &#8220;hot&#8221; refreshes. For example, <code>XmlWebApplicationContext</code> supports hot refreshes, but
  <code>GenericApplicationContext</code> does not.</p></td>
</tr>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>ContextStartedEvent</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock">Published when the <code>ApplicationContext</code> is started by using the <code>start()</code> method on the
  <code>ConfigurableApplicationContext</code> interface. Here, &#8220;started&#8221; means that all <code>Lifecycle</code>
  beans receive an explicit start signal. Typically, this signal is used to restart beans
  after an explicit stop, but it may also be used to start components that have not been
  configured for autostart (for example, components that have not already started on
  initialization).</p></td>
</tr>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>ContextStoppedEvent</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock">Published when the <code>ApplicationContext</code> is stopped by using the <code>stop()</code> method on the
  <code>ConfigurableApplicationContext</code> interface. Here, &#8220;stopped&#8221; means that all <code>Lifecycle</code>
  beans receive an explicit stop signal. A stopped context may be restarted through a
  <code>start()</code> call.</p></td>
</tr>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>ContextClosedEvent</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock">Published when the <code>ApplicationContext</code> is being closed by using the <code>close()</code> method
  on the <code>ConfigurableApplicationContext</code> interface or via a JVM shutdown hook. Here,
  "closed" means that all singleton beans will be destroyed. Once the context is closed,
  it reaches its end of life and cannot be refreshed or restarted.</p></td>
</tr>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>RequestHandledEvent</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock">A web-specific event telling all beans that an HTTP request has been serviced. This
  event is published after the request is complete. This event is only applicable to
  web applications that use Spring&#8217;s <code>DispatcherServlet</code>.</p></td>
</tr>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>ServletRequestHandledEvent</code></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock">A subclass of <code>RequestHandledEvent</code> that adds Servlet-specific context information.</p></td>
</tr>
</tbody>
</table>
<div class="paragraph">
<p>You can also create and publish your own custom events. The following example shows a
simple class that extends Spring&#8217;s <code>ApplicationEvent</code> base class:</p>
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
</ul>
</div>
<div id="_tabs_4_java--panel" class="tabpanel" aria-labelledby="_tabs_4_java">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">public class BlockedListEvent extends ApplicationEvent {

	private final String address;
	private final String content;

	public BlockedListEvent(Object source, String address, String content) {
		super(source);
		this.address = address;
		this.content = content;
	}

	// accessor and other methods...
}</code></pre>
</div>
</div>
</div>
<div id="_tabs_4_kotlin--panel" class="tabpanel" aria-labelledby="_tabs_4_kotlin">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-kotlin hljs" data-lang="kotlin">class BlockedListEvent(source: Any,
					val address: String,
					val content: String) : ApplicationEvent(source)</code></pre>
</div>
</div>
</div>
</div>
</div>
<div class="paragraph">
<p>To publish a custom <code>ApplicationEvent</code>, call the <code>publishEvent()</code> method on an
<code>ApplicationEventPublisher</code>. Typically, this is done by creating a class that implements
<code>ApplicationEventPublisherAware</code> and registering it as a Spring bean. The following
example shows such a class:</p>
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
</ul>
</div>
<div id="_tabs_5_java--panel" class="tabpanel" aria-labelledby="_tabs_5_java">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">public class EmailService implements ApplicationEventPublisherAware {

	private List&lt;String&gt; blockedList;
	private ApplicationEventPublisher publisher;

	public void setBlockedList(List&lt;String&gt; blockedList) {
		this.blockedList = blockedList;
	}

	public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
		this.publisher = publisher;
	}

	public void sendEmail(String address, String content) {
		if (blockedList.contains(address)) {
			publisher.publishEvent(new BlockedListEvent(this, address, content));
			return;
		}
		// send email...
	}
}</code></pre>
</div>
</div>
</div>
<div id="_tabs_5_kotlin--panel" class="tabpanel" aria-labelledby="_tabs_5_kotlin">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-kotlin hljs" data-lang="kotlin">class EmailService : ApplicationEventPublisherAware {

	private lateinit var blockedList: List&lt;String&gt;
	private lateinit var publisher: ApplicationEventPublisher

	fun setBlockedList(blockedList: List&lt;String&gt;) {
		this.blockedList = blockedList
	}

	override fun setApplicationEventPublisher(publisher: ApplicationEventPublisher) {
		this.publisher = publisher
	}

	fun sendEmail(address: String, content: String) {
		if (blockedList!!.contains(address)) {
			publisher!!.publishEvent(BlockedListEvent(this, address, content))
			return
		}
		// send email...
	}
}</code></pre>
</div>
</div>
</div>
</div>
</div>
<div class="paragraph">
<p>At configuration time, the Spring container detects that <code>EmailService</code> implements
<code>ApplicationEventPublisherAware</code> and automatically calls
<code>setApplicationEventPublisher()</code>. In reality, the parameter passed in is the Spring
container itself. You are interacting with the application context through its
<code>ApplicationEventPublisher</code> interface.</p>
</div>
<div class="paragraph">
<p>To receive the custom <code>ApplicationEvent</code>, you can create a class that implements
<code>ApplicationListener</code> and register it as a Spring bean. The following example
shows such a class:</p>
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
</ul>
</div>
<div id="_tabs_6_java--panel" class="tabpanel" aria-labelledby="_tabs_6_java">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">public class BlockedListNotifier implements ApplicationListener&lt;BlockedListEvent&gt; {

	private String notificationAddress;

	public void setNotificationAddress(String notificationAddress) {
		this.notificationAddress = notificationAddress;
	}

	public void onApplicationEvent(BlockedListEvent event) {
		// notify appropriate parties via notificationAddress...
	}
}</code></pre>
</div>
</div>
</div>
<div id="_tabs_6_kotlin--panel" class="tabpanel" aria-labelledby="_tabs_6_kotlin">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-kotlin hljs" data-lang="kotlin">class BlockedListNotifier : ApplicationListener&lt;BlockedListEvent&gt; {

	lateinit var notificationAddress: String

	override fun onApplicationEvent(event: BlockedListEvent) {
		// notify appropriate parties via notificationAddress...
	}
}</code></pre>
</div>
</div>
</div>
</div>
</div>
<div class="paragraph">
<p>Notice that <code>ApplicationListener</code> is generically parameterized with the type of your custom event (<code>BlockedListEvent</code> in the preceding example).
This means that the <code>onApplicationEvent()</code> method can remain type-safe, avoiding any need for downcasting.
You can register as many event listeners as you wish, but note that, by default, event listeners receive events synchronously.
This means that the <code>publishEvent()</code> method blocks until all listeners have finished processing the event.
One advantage of this synchronous and single-threaded approach is that, when a listener receives an event,
it operates inside the transaction context of the publisher if a transaction context is available.
If another strategy for event publication becomes necessary, for example, asynchronous event processing by default,
see the javadoc for Spring&#8217;s <a href="https://docs.spring.io/spring-framework/docs/7.0.9/javadoc-api/org/springframework/context/event/ApplicationEventMulticaster.html"><code>ApplicationEventMulticaster</code></a> interface
and <a href="https://docs.spring.io/spring-framework/docs/7.0.9/javadoc-api/org/springframework/context/event/SimpleApplicationEventMulticaster.html"><code>SimpleApplicationEventMulticaster</code></a> implementation
for configuration options which can be applied to a custom "applicationEventMulticaster" bean definition.
In these cases, ThreadLocals and logging context are not propagated for the event processing.
See <a href="../../integration/observability.html#observability.application-events" class="xref page">the <code>@EventListener</code> Observability section</a>
for more information on Observability concerns.</p>
</div>
<div class="paragraph">
<p>The following example shows the bean definitions used to register and configure each of
the classes above:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-xml hljs" data-lang="xml">&lt;bean id="emailService" class="example.EmailService"&gt;
	&lt;property name="blockedList"&gt;
		&lt;list&gt;
			&lt;value&gt;<a href="/cdn-cgi/l/email-protection" class="__cf_email__" data-cfemail="f69d98998198d88586979b9b9384b6938e979b869a93d8998491">[email&#160;protected]</a>&lt;/value&gt;
			&lt;value&gt;<a href="/cdn-cgi/l/email-protection" class="__cf_email__" data-cfemail="94fffafbe3fabafcf5f7fff1e6d4f1ecf5f9e4f8f1bafbe6f3">[email&#160;protected]</a>&lt;/value&gt;
			&lt;value&gt;<a href="/cdn-cgi/l/email-protection" class="__cf_email__" data-cfemail="f2989d9a9cdc969d97b2978a939f829e97dc9d8095">[email&#160;protected]</a>&lt;/value&gt;
		&lt;/list&gt;
	&lt;/property&gt;
&lt;/bean&gt;

&lt;bean id="blockedListNotifier" class="example.BlockedListNotifier"&gt;
	&lt;property name="notificationAddress" value="<a href="/cdn-cgi/l/email-protection" class="__cf_email__" data-cfemail="57353b38343c32333b3e242317322f363a273b3279382530">[email&#160;protected]</a>"/&gt;
&lt;/bean&gt;

&lt;!-- optional: a custom ApplicationEventMulticaster definition --&gt;
&lt;bean id="applicationEventMulticaster" class="org.springframework.context.event.SimpleApplicationEventMulticaster"&gt;
	&lt;property name="taskExecutor" ref="..."/&gt;
	&lt;property name="errorHandler" ref="..."/&gt;
&lt;/bean&gt;</code></pre>
</div>
</div>
<div class="paragraph">
<p>Putting it all together, when the <code>sendEmail()</code> method of the <code>emailService</code> bean is
called, if there are any email messages that should be blocked, a custom event of type
<code>BlockedListEvent</code> is published. The <code>blockedListNotifier</code> bean is registered as an
<code>ApplicationListener</code> and receives the <code>BlockedListEvent</code>, at which point it can
notify appropriate parties.</p>
</div>
<div class="admonitionblock note">
<table>
<tr>
<td class="icon">
<i class="fa icon-note" title="Note"></i>
</td>
<td class="content">
Spring&#8217;s eventing mechanism is designed for simple communication between Spring beans
within the same application context. However, for more sophisticated enterprise
integration needs, the separately maintained
<a href="https://spring.io/projects/spring-integration/">Spring Integration</a> project provides
complete support for building lightweight,
<a href="https://www.enterpriseintegrationpatterns.com">pattern-oriented</a>, event-driven
architectures that build upon the well-known Spring programming model.
</td>
</tr>
</table>
</div>
<div class="sect2">
<h3 id="context-functionality-events-annotation"><a class="anchor" href="#context-functionality-events-annotation"></a>Annotation-based Event Listeners</h3>
<div class="paragraph">
<p>You can register an event listener on any method of a managed bean by using the
<code>@EventListener</code> annotation. The <code>BlockedListNotifier</code> can be rewritten as follows:</p>
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
</ul>
</div>
<div id="_tabs_7_java--panel" class="tabpanel" aria-labelledby="_tabs_7_java">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">public class BlockedListNotifier {

	private String notificationAddress;

	public void setNotificationAddress(String notificationAddress) {
		this.notificationAddress = notificationAddress;
	}

	@EventListener
	public void processBlockedListEvent(BlockedListEvent event) {
		// notify appropriate parties via notificationAddress...
	}
}</code></pre>
</div>
</div>
</div>
<div id="_tabs_7_kotlin--panel" class="tabpanel" aria-labelledby="_tabs_7_kotlin">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-kotlin hljs" data-lang="kotlin">class BlockedListNotifier {

	lateinit var notificationAddress: String

	@EventListener
	fun processBlockedListEvent(event: BlockedListEvent) {
		// notify appropriate parties via notificationAddress...
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
Do not define such beans to be lazy as the <code>ApplicationContext</code> will honor that and will not register the method to listen to events.
</td>
</tr>
</table>
</div>
<div class="paragraph">
<p>The method signature once again declares the event type to which it listens,
but, this time, with a flexible name and without implementing a specific listener interface.
The event type can also be narrowed through generics as long as the actual event type
resolves your generic parameter in its implementation hierarchy.</p>
</div>
<div class="paragraph">
<p>If your method should listen to several events or if you want to define it with no
parameter at all, the event types can also be specified on the annotation itself. The
following example shows how to do so:</p>
</div>
<div id="_tabs_8" class="openblock tabs is-sync is-loading">
<div class="content">
<div class="ulist tablist">
<ul>
<li id="_tabs_8_java" class="tab">
<p>Java</p>
</li>
<li id="_tabs_8_kotlin" class="tab">
<p>Kotlin</p>
</li>
</ul>
</div>
<div id="_tabs_8_java--panel" class="tabpanel" aria-labelledby="_tabs_8_java">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@EventListener({ContextStartedEvent.class, ContextRefreshedEvent.class})
public void handleContextStart() {
	// ...
}</code></pre>
</div>
</div>
</div>
<div id="_tabs_8_kotlin--panel" class="tabpanel" aria-labelledby="_tabs_8_kotlin">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-kotlin hljs" data-lang="kotlin">@EventListener(ContextStartedEvent::class, ContextRefreshedEvent::class)
fun handleContextStart() {
	// ...
}</code></pre>
</div>
</div>
</div>
</div>
</div>
<div class="paragraph">
<p>It is also possible to add additional runtime filtering by using the <code>condition</code> attribute
of the annotation that defines a <a href="../expressions.html" class="xref page"><code>SpEL</code> expression</a>, which should match
to actually invoke the method for a particular event.</p>
</div>
<div class="paragraph">
<p>The following example shows how our notifier can be rewritten to be invoked only if the
<code>content</code> attribute of the event is equal to <code>my-event</code>:</p>
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
</ul>
</div>
<div id="_tabs_9_java--panel" class="tabpanel" aria-labelledby="_tabs_9_java">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@EventListener(condition = "#blEvent.content == 'my-event'")
public void processBlockedListEvent(BlockedListEvent blEvent) {
	// notify appropriate parties via notificationAddress...
}</code></pre>
</div>
</div>
</div>
<div id="_tabs_9_kotlin--panel" class="tabpanel" aria-labelledby="_tabs_9_kotlin">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-kotlin hljs" data-lang="kotlin">@EventListener(condition = "#blEvent.content == 'my-event'")
fun processBlockedListEvent(blEvent: BlockedListEvent) {
	// notify appropriate parties via notificationAddress...
}</code></pre>
</div>
</div>
</div>
</div>
</div>
<div class="paragraph">
<p>Each <code>SpEL</code> expression evaluates against a dedicated context. The following table lists the
items made available to the context so that you can use them for conditional event processing:</p>
</div>
<table id="context-functionality-events-annotation-tbl" class="tableblock frame-all grid-all stripes-odd stretch">
<caption class="title">Table 2. Event metadata available in SpEL expressions</caption>
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
<td class="tableblock halign-left valign-top"><p class="tableblock">Event</p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock">root object</p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock">The actual <code>ApplicationEvent</code>.</p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>#root.event</code> or <code>event</code></p></td>
</tr>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock">Arguments array</p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock">root object</p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock">The arguments (as an object array) used to invoke the method.</p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>#root.args</code> or <code>args</code>; <code>args[0]</code> to access the first argument, etc.</p></td>
</tr>
<tr>
<td class="tableblock halign-left valign-top"><p class="tableblock"><em>Argument name</em></p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock">evaluation context</p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock">The name of a particular method argument. If the names are not available
  (for example, because the code was compiled without the <code>-parameters</code> flag), individual
  arguments are also available using the <code>#a&lt;#arg&gt;</code> syntax where <code>&lt;#arg&gt;</code> stands for the
  argument index (starting from 0).</p></td>
<td class="tableblock halign-left valign-top"><p class="tableblock"><code>#blEvent</code> or <code>#a0</code> (you can also use <code>#p0</code> or <code>#p&lt;#arg&gt;</code> parameter notation as an alias)</p></td>
</tr>
</tbody>
</table>
<div class="paragraph">
<p>Note that <code>#root.event</code> gives you access to the underlying event, even if your method
signature actually refers to an arbitrary object that was published.</p>
</div>
<div class="paragraph">
<p>If you need to publish an event as the result of processing another event, you can change the
method signature to return the event that should be published, as the following example shows:</p>
</div>
<div id="_tabs_10" class="openblock tabs is-sync is-loading">
<div class="content">
<div class="ulist tablist">
<ul>
<li id="_tabs_10_java" class="tab">
<p>Java</p>
</li>
<li id="_tabs_10_kotlin" class="tab">
<p>Kotlin</p>
</li>
</ul>
</div>
<div id="_tabs_10_java--panel" class="tabpanel" aria-labelledby="_tabs_10_java">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@EventListener
public ListUpdateEvent handleBlockedListEvent(BlockedListEvent event) {
	// notify appropriate parties via notificationAddress and
	// then publish a ListUpdateEvent...
}</code></pre>
</div>
</div>
</div>
<div id="_tabs_10_kotlin--panel" class="tabpanel" aria-labelledby="_tabs_10_kotlin">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-kotlin hljs" data-lang="kotlin">@EventListener
fun handleBlockedListEvent(event: BlockedListEvent): ListUpdateEvent {
	// notify appropriate parties via notificationAddress and
	// then publish a ListUpdateEvent...
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
This feature is not supported for
<a href="#context-functionality-events-async">asynchronous listeners</a>.
</td>
</tr>
</table>
</div>
<div class="paragraph">
<p>The <code>handleBlockedListEvent()</code> method publishes a new <code>ListUpdateEvent</code> for every
<code>BlockedListEvent</code> that it handles. If you need to publish several events, you can return
a <code>Collection</code> or an array of events instead.</p>
</div>
</div>
<div class="sect2">
<h3 id="context-functionality-events-async"><a class="anchor" href="#context-functionality-events-async"></a>Asynchronous Listeners</h3>
<div class="paragraph">
<p>If you want a particular listener to process events asynchronously, you can reuse the
<a href="../../integration/scheduling.html#scheduling-annotation-support-async" class="xref page">regular <code>@Async</code> support</a>.
The following example shows how to do so:</p>
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
</ul>
</div>
<div id="_tabs_11_java--panel" class="tabpanel" aria-labelledby="_tabs_11_java">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@EventListener
@Async
public void processBlockedListEvent(BlockedListEvent event) {
	// BlockedListEvent is processed in a separate thread
}</code></pre>
</div>
</div>
</div>
<div id="_tabs_11_kotlin--panel" class="tabpanel" aria-labelledby="_tabs_11_kotlin">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-kotlin hljs" data-lang="kotlin">@EventListener
@Async
fun processBlockedListEvent(event: BlockedListEvent) {
	// BlockedListEvent is processed in a separate thread
}</code></pre>
</div>
</div>
</div>
</div>
</div>
<div class="paragraph">
<p>Be aware of the following limitations when using asynchronous events:</p>
</div>
<div class="ulist">
<ul>
<li>
<p>If an asynchronous event listener throws an <code>Exception</code>, it is not propagated to the
caller. See
<a href="https://docs.spring.io/spring-framework/docs/7.0.9/javadoc-api/org/springframework/aop/interceptor/AsyncUncaughtExceptionHandler.html"><code>AsyncUncaughtExceptionHandler</code></a>
for more details.</p>
</li>
<li>
<p>Asynchronous event listener methods cannot publish a subsequent event by returning a
value. If you need to publish another event as the result of the processing, inject an
<a href="https://docs.spring.io/spring-framework/docs/7.0.9/javadoc-api/org/springframework/context/ApplicationEventPublisher.html"><code>ApplicationEventPublisher</code></a>
to publish the event manually.</p>
</li>
<li>
<p>ThreadLocals and logging context are not propagated by default for the event processing.
See <a href="../../integration/observability.html#observability.application-events" class="xref page">the <code>@EventListener</code> Observability section</a>
for more information on Observability concerns.</p>
</li>
</ul>
</div>
</div>
<div class="sect2">
<h3 id="context-functionality-events-order"><a class="anchor" href="#context-functionality-events-order"></a>Ordering Listeners</h3>
<div class="paragraph">
<p>If you need one listener to be invoked before another one, you can add the <code>@Order</code>
annotation to the method declaration, as the following example shows:</p>
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
</ul>
</div>
<div id="_tabs_12_java--panel" class="tabpanel" aria-labelledby="_tabs_12_java">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@EventListener
@Order(42)
public void processBlockedListEvent(BlockedListEvent event) {
	// notify appropriate parties via notificationAddress...
}</code></pre>
</div>
</div>
</div>
<div id="_tabs_12_kotlin--panel" class="tabpanel" aria-labelledby="_tabs_12_kotlin">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-kotlin hljs" data-lang="kotlin">@EventListener
@Order(42)
fun processBlockedListEvent(event: BlockedListEvent) {
	// notify appropriate parties via notificationAddress...
}</code></pre>
</div>
</div>
</div>
</div>
</div>
</div>
<div class="sect2">
<h3 id="context-functionality-events-generics"><a class="anchor" href="#context-functionality-events-generics"></a>Generic Events</h3>
<div class="paragraph">
<p>You can also use generics to further define the structure of your event. Consider using an
<code>EntityCreatedEvent&lt;T&gt;</code> where <code>T</code> is the type of the actual entity that got created. For example, you
can create the following listener definition to receive only <code>EntityCreatedEvent</code> for a
<code>Person</code>:</p>
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
</ul>
</div>
<div id="_tabs_13_java--panel" class="tabpanel" aria-labelledby="_tabs_13_java">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@EventListener
public void onPersonCreated(EntityCreatedEvent&lt;Person&gt; event) {
	// ...
}</code></pre>
</div>
</div>
</div>
<div id="_tabs_13_kotlin--panel" class="tabpanel" aria-labelledby="_tabs_13_kotlin">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-kotlin hljs" data-lang="kotlin">@EventListener
fun onPersonCreated(event: EntityCreatedEvent&lt;Person&gt;) {
	// ...
}</code></pre>
</div>
</div>
</div>
</div>
</div>
<div class="paragraph">
<p>Due to type erasure, this works only if the event that is fired resolves the generic
parameters on which the event listener filters (that is, something like
<code>class PersonCreatedEvent extends EntityCreatedEvent&lt;Person&gt; { &#8230;&#8203; }</code>).</p>
</div>
<div class="paragraph">
<p>In certain circumstances, this may become quite tedious if all events follow the same
structure (as should be the case for the event in the preceding example). In such a case,
you can implement <code>ResolvableTypeProvider</code> to guide the framework beyond what the runtime
environment provides. The following event shows how to do so:</p>
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
</ul>
</div>
<div id="_tabs_14_java--panel" class="tabpanel" aria-labelledby="_tabs_14_java">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">public class EntityCreatedEvent&lt;T&gt; extends ApplicationEvent implements ResolvableTypeProvider {

	public EntityCreatedEvent(T entity) {
		super(entity);
	}

	@Override
	public ResolvableType getResolvableType() {
		return ResolvableType.forClassWithGenerics(getClass(), ResolvableType.forInstance(getSource()));
	}
}</code></pre>
</div>
</div>
</div>
<div id="_tabs_14_kotlin--panel" class="tabpanel" aria-labelledby="_tabs_14_kotlin">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-kotlin hljs" data-lang="kotlin">class EntityCreatedEvent&lt;T&gt;(entity: T) : ApplicationEvent(entity), ResolvableTypeProvider {

	override fun getResolvableType(): ResolvableType? {
		return ResolvableType.forClassWithGenerics(javaClass, ResolvableType.forInstance(getSource()))
	}
}</code></pre>
</div>
</div>
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
This works not only for <code>ApplicationEvent</code> but any arbitrary object that you send as
an event.
</td>
</tr>
</table>
</div>
<div class="paragraph">
<p>Finally, as with classic <code>ApplicationListener</code> implementations, the actual multicasting
happens via a context-wide <code>ApplicationEventMulticaster</code> at runtime. By default, this is a
<code>SimpleApplicationEventMulticaster</code> with synchronous event publication in the caller thread.
This can be replaced/customized through an "applicationEventMulticaster" bean definition,
for example, for processing all events asynchronously and/or for handling listener exceptions:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">@Bean
ApplicationEventMulticaster applicationEventMulticaster() {
	SimpleApplicationEventMulticaster multicaster = new SimpleApplicationEventMulticaster();
	multicaster.setTaskExecutor(...);
	multicaster.setErrorHandler(...);
	return multicaster;
}</code></pre>
</div>
</div>
</div>
</div>
</div>
<div class="sect1">
<h2 id="context-functionality-resources"><a class="anchor" href="#context-functionality-resources"></a>Convenient Access to Low-level Resources</h2>
<div class="sectionbody">
<div class="paragraph">
<p>For optimal usage and understanding of application contexts, you should familiarize
yourself with Spring&#8217;s <code>Resource</code> abstraction, as described in
<a href="../resources.html" class="xref page">Resources</a>.</p>
</div>
<div class="paragraph">
<p>An application context is a <code>ResourceLoader</code>, which can be used to load <code>Resource</code> objects.
A <code>Resource</code> is essentially a more feature rich version of the JDK <code>java.net.URL</code> class.
In fact, implementations of <code>Resource</code> wrap an instance of <code>java.net.URL</code>, where
appropriate. A <code>Resource</code> can obtain low-level resources from almost any location in a
transparent fashion, including from the classpath, a filesystem location, anywhere
describable with a standard URL, and some other variations. If the resource location
string is a simple path without any special prefixes, where those resources come from is
specific and appropriate to the actual application context type.</p>
</div>
<div class="paragraph">
<p>You can configure a bean deployed into the application context to implement the special
callback interface, <code>ResourceLoaderAware</code>, to be automatically called back at
initialization time with the application context itself passed in as the <code>ResourceLoader</code>.
You can also expose properties of type <code>Resource</code>, to be used to access static resources.
They are injected into it like any other properties. You can specify those <code>Resource</code>
properties as simple <code>String</code> paths and rely on automatic conversion from those text
strings to actual <code>Resource</code> objects when the bean is deployed.</p>
</div>
<div class="paragraph">
<p>The location path or paths supplied to an <code>ApplicationContext</code> constructor are actually
resource strings and, in simple form, are treated appropriately according to the specific
context implementation. For example <code>ClassPathXmlApplicationContext</code> treats a simple
location path as a classpath location. You can also use location paths (resource strings)
with special prefixes to force loading of definitions from the classpath or a URL,
regardless of the actual context type.</p>
</div>
</div>
</div>
<div class="sect1">
<h2 id="context-functionality-startup"><a class="anchor" href="#context-functionality-startup"></a>Application Startup Tracking</h2>
<div class="sectionbody">
<div class="paragraph">
<p>The <code>ApplicationContext</code> manages the lifecycle of Spring applications and provides a rich
programming model around components. As a result, complex applications can have equally
complex component graphs and startup phases.</p>
</div>
<div class="paragraph">
<p>Tracking the application startup steps with specific metrics can help understand where
time is being spent during the startup phase, but it can also be used as a way to better
understand the context lifecycle as a whole.</p>
</div>
<div class="paragraph">
<p>The <code>AbstractApplicationContext</code> (and its subclasses) is instrumented with an
<code>ApplicationStartup</code>, which collects <code>StartupStep</code> data about various startup phases:</p>
</div>
<div class="ulist">
<ul>
<li>
<p>application context lifecycle (base packages scanning, config classes management)</p>
</li>
<li>
<p>beans lifecycle (instantiation, smart initialization, post processing)</p>
</li>
<li>
<p>application events processing</p>
</li>
</ul>
</div>
<div class="paragraph">
<p>Here is an example of instrumentation in the <code>AnnotationConfigApplicationContext</code>:</p>
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
</ul>
</div>
<div id="_tabs_15_java--panel" class="tabpanel" aria-labelledby="_tabs_15_java">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-java hljs" data-lang="java">// create a startup step and start recording
try (StartupStep scanPackages = getApplicationStartup().start("spring.context.base-packages.scan")) {
       // add tagging information to the current step
       scanPackages.tag("packages", () -&gt; Arrays.toString(basePackages));
       // perform the actual phase we're instrumenting
       this.scanner.scan(basePackages);
}</code></pre>
</div>
</div>
</div>
<div id="_tabs_15_kotlin--panel" class="tabpanel" aria-labelledby="_tabs_15_kotlin">
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-kotlin hljs" data-lang="kotlin">// create a startup step and start recording
try (val scanPackages = getApplicationStartup().start("spring.context.base-packages.scan")) {
       // add tagging information to the current step
       scanPackages.tag("packages", () -&gt; Arrays.toString(basePackages));
       // perform the actual phase we're instrumenting
       this.scanner.scan(basePackages);
}</code></pre>
</div>
</div>
</div>
</div>
</div>
<div class="paragraph">
<p>The application context is already instrumented with multiple steps.
Once recorded, these startup steps can be collected, displayed and analyzed with specific tools.
For a complete list of existing startup steps, you can check out the
<a href="../appendix/application-startup-steps.html" class="xref page">dedicated appendix section</a>.</p>
</div>
<div class="paragraph">
<p>The default <code>ApplicationStartup</code> implementation is a no-op variant, for minimal overhead.
This means no metrics will be collected during application startup by default.
Spring Framework ships with an implementation for tracking startup steps with Java Flight Recorder:
<code>FlightRecorderApplicationStartup</code>. To use this variant, you must configure an instance of it
to the <code>ApplicationContext</code> as soon as it&#8217;s been created.</p>
</div>
<div class="paragraph">
<p>Developers can also use the <code>ApplicationStartup</code> infrastructure if they&#8217;re providing their own
<code>AbstractApplicationContext</code> subclass, or if they wish to collect more precise data.</p>
</div>
<div class="admonitionblock warning">
<table>
<tr>
<td class="icon">
<i class="fa icon-warning" title="Warning"></i>
</td>
<td class="content">
<code>ApplicationStartup</code> is meant to be only used during application startup and for
the core container; this is by no means a replacement for Java profilers or
metrics libraries like <a href="https://micrometer.io">Micrometer</a>.
</td>
</tr>
</table>
</div>
<div class="paragraph">
<p>To start collecting custom <code>StartupStep</code>, components can either get the <code>ApplicationStartup</code>
instance from the application context directly, make their component implement <code>ApplicationStartupAware</code>,
or ask for the <code>ApplicationStartup</code> type on any injection point.</p>
</div>
<div class="admonitionblock note">
<table>
<tr>
<td class="icon">
<i class="fa icon-note" title="Note"></i>
</td>
<td class="content">
Developers should not use the <code>"spring.*"</code> namespace when creating custom startup steps.
This namespace is reserved for internal Spring usage and is subject to change.
</td>
</tr>
</table>
</div>
</div>
</div>
<div class="sect1">
<h2 id="context-create"><a class="anchor" href="#context-create"></a>Convenient ApplicationContext Instantiation for Web Applications</h2>
<div class="sectionbody">
<div class="paragraph">
<p>You can create <code>ApplicationContext</code> instances declaratively by using, for example, a
<code>ContextLoader</code>. Of course, you can also create <code>ApplicationContext</code> instances
programmatically by using one of the <code>ApplicationContext</code> implementations.</p>
</div>
<div class="paragraph">
<p>You can register an <code>ApplicationContext</code> by using the <code>ContextLoaderListener</code>, as the
following example shows:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlightjs highlight"><code class="language-xml hljs" data-lang="xml">&lt;context-param&gt;
	&lt;param-name&gt;contextConfigLocation&lt;/param-name&gt;
	&lt;param-value&gt;/WEB-INF/daoContext.xml /WEB-INF/applicationContext.xml&lt;/param-value&gt;
&lt;/context-param&gt;

&lt;listener&gt;
	&lt;listener-class&gt;org.springframework.web.context.ContextLoaderListener&lt;/listener-class&gt;
&lt;/listener&gt;</code></pre>
</div>
</div>
<div class="paragraph">
<p>The listener inspects the <code>contextConfigLocation</code> parameter. If the parameter does not
exist, the listener uses <code>/WEB-INF/applicationContext.xml</code> as a default. When the
parameter does exist, the listener separates the <code>String</code> by using predefined
delimiters (comma, semicolon, and whitespace) and uses the values as locations where
application contexts are searched. Ant-style path patterns are supported as well.
Examples are <code>/WEB-INF/*Context.xml</code> (for all files with names that end with
<code>Context.xml</code> and that reside in the <code>WEB-INF</code> directory) and <code>/WEB-INF/**/*Context.xml</code>
(for all such files in any subdirectory of <code>WEB-INF</code>).</p>
</div>
</div>
</div>
<div class="sect1">
<h2 id="context-deploy-rar"><a class="anchor" href="#context-deploy-rar"></a>Deploying a Spring <code>ApplicationContext</code> as a Jakarta EE RAR File</h2>
<div class="sectionbody">
<div class="paragraph">
<p>It is possible to deploy a Spring <code>ApplicationContext</code> as a RAR file, encapsulating the
context and all of its required bean classes and library JARs in a Jakarta EE RAR deployment
unit. This is the equivalent of bootstrapping a stand-alone <code>ApplicationContext</code> (only hosted
in Jakarta EE environment) being able to access the Jakarta EE servers facilities. RAR deployment
is a more natural alternative to a scenario of deploying a headless WAR file&#8201;&#8212;&#8201;in effect,
a WAR file without any HTTP entry points that is used only for bootstrapping a Spring
<code>ApplicationContext</code> in a Jakarta EE environment.</p>
</div>
<div class="paragraph">
<p>RAR deployment is ideal for application contexts that do not need HTTP entry points but
rather consist only of message endpoints and scheduled jobs. Beans in such a context can
use application server resources such as the JTA transaction manager and JNDI-bound JDBC
<code>DataSource</code> instances and JMS <code>ConnectionFactory</code> instances and can also register with
the platform&#8217;s JMX server&#8201;&#8212;&#8201;all through Spring&#8217;s standard transaction management and JNDI
and JMX support facilities. Application components can also interact with the application
server&#8217;s JCA <code>WorkManager</code> through Spring&#8217;s <code>TaskExecutor</code> abstraction.</p>
</div>
<div class="paragraph">
<p>See the javadoc of the
<a href="https://docs.spring.io/spring-framework/docs/7.0.9/javadoc-api/org/springframework/jca/context/SpringContextResourceAdapter.html"><code>SpringContextResourceAdapter</code></a>
class for the configuration details involved in RAR deployment.</p>
</div>
<div class="paragraph">
<p>For a simple deployment of a Spring ApplicationContext as a Jakarta EE RAR file:</p>
</div>
<div class="olist arabic">
<ol class="arabic">
<li>
<p>Package
all application classes into a RAR file (which is a standard JAR file with a different
file extension).</p>
</li>
<li>
<p>Add all required library JARs into the root of the RAR archive.</p>
</li>
<li>
<p>Add a
<code>META-INF/ra.xml</code> deployment descriptor (as shown in the
<a href="https://docs.spring.io/spring-framework/docs/7.0.9/javadoc-api/org/springframework/jca/context/SpringContextResourceAdapter.html">javadoc for <code>SpringContextResourceAdapter</code></a>)
and the corresponding Spring XML bean definition file(s) (typically
<code>META-INF/applicationContext.xml</code>).</p>
</li>
<li>
<p>Drop the resulting RAR file into your
application server&#8217;s deployment directory.</p>
</li>
</ol>
</div>
<div class="admonitionblock note">
<table>
<tr>
<td class="icon">
<i class="fa icon-note" title="Note"></i>
</td>
<td class="content">
Such RAR deployment units are usually self-contained. They do not expose components
to the outside world, not even to other modules of the same application. Interaction with a
RAR-based <code>ApplicationContext</code> usually occurs through JMS destinations that it shares with
other modules. A RAR-based <code>ApplicationContext</code> may also, for example, schedule some jobs
or react to new files in the file system (or the like). If it needs to allow synchronous
access from the outside, it could (for example) export RMI endpoints, which may be used
by other application modules on the same machine.
</td>
</tr>
</table>
</div>
</div>
</div>
<nav class="pagination">
  <span class="prev"><a href="context-load-time-weaver.html">Registering a <code>LoadTimeWeaver</code></a></span>
  <span class="next"><a href="beanfactory.html">The <code>BeanFactory</code> API</a></span>
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
    <a href="context-introduction.html">
      7.0.9
    </a>
  </li>
  <li class="version">
    <a href="../../6.2/core/beans/context-introduction.html">
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
    <a href="../../7.1/core/beans/context-introduction.html">
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
    <a href="../../7.1-SNAPSHOT/core/beans/context-introduction.html">
      7.1.0-SNAPSHOT
    </a>
  </li>
  <li class="version">
    <a href="../../7.0-SNAPSHOT/core/beans/context-introduction.html">
      7.0.10-SNAPSHOT
    </a>
  </li>
  <li class="version">
    <a href="../../6.2-SNAPSHOT/core/beans/context-introduction.html">
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
        <p class="smallest antialiased">Copyright © 2005 - <script data-cfasync="false" src="/cdn-cgi/scripts/5c5dd728/cloudflare-static/email-decode.min.js"></script><script>var d = new Date();
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

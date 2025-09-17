<section class="executive-summary">
  <h2>Executive Summary</h2>
  <div class="executive-content">
    <p>This report presents the results of comprehensive compatibility testing between
    WebAssembly runtime implementations. The analysis covers ${summary.totalTests} tests
    with an overall compatibility score of ${summary.compatibilityScore?string("0.00")}.</p>

    <#if summary.testsWithIssues gt 0>
      <div class="executive-concerns">
        <h3>Key Concerns</h3>
        <ul>
          <#if summary.testsWithBehavioralIssues gt 0>
            <li>${summary.testsWithBehavioralIssues} tests showed behavioral compatibility issues</li>
          </#if>
          <#if summary.testsWithPerformanceIssues gt 0>
            <li>${summary.testsWithPerformanceIssues} tests showed significant performance differences</li>
          </#if>
          <#if summary.testsWithCoverageGaps gt 0>
            <li>${summary.testsWithCoverageGaps} tests revealed coverage gaps</li>
          </#if>
        </ul>
      </div>
    </#if>

    <#if summary.highPriorityRecommendations gt 0>
      <div class="executive-actions">
        <h3>Immediate Actions Required</h3>
        <p>${summary.highPriorityRecommendations} high-priority recommendations require immediate attention.</p>
      </div>
    </#if>
  </div>
</section>
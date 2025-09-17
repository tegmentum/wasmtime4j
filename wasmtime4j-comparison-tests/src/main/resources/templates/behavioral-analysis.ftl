<section class="behavioral-analysis">
  <h2>Behavioral Analysis</h2>
  <#if testsWithBehavioralIssues?has_content>
    <div class="behavioral-issues">
      <h3>Tests with Behavioral Issues</h3>
      <#list testsWithBehavioralIssues as testName, testResult>
        <div class="test-issue">
          <h4>${testName}</h4>
          <#if testResult.behavioralResults.present>
            <div class="behavioral-verdict verdict-${testResult.behavioralResults.get().verdict?lower_case}">
              ${testResult.behavioralResults.get().verdict.description}
            </div>
            <div class="consistency-score">
              Consistency Score: ${testResult.behavioralResults.get().consistencyScore?string("0.00")}
            </div>
            <#if testResult.behavioralResults.get().discrepancies?has_content>
              <div class="discrepancies">
                <h5>Discrepancies</h5>
                <#list testResult.behavioralResults.get().discrepancies as discrepancy>
                  <div class="discrepancy severity-${discrepancy.severity?lower_case}">
                    <span class="discrepancy-type">${discrepancy.discrepancyType}</span>
                    <span class="discrepancy-description">${discrepancy.description}</span>
                  </div>
                </#list>
              </div>
            </#if>
          </#if>
        </div>
      </#list>
    </div>
  <#else>
    <div class="no-issues">
      <p>No behavioral issues detected in any tests.</p>
    </div>
  </#if>
</section>
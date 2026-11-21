package me.skywars2.game.genie;

/** Connection loss must not permanently disable the once-per-minute renewal. */
public final class GenieSessionChecks {
    public static void main(String[] args) {
        for (var issue : new GenieGeminiWebTransport.SessionIssue[]{
                GenieGeminiWebTransport.SessionIssue.NETWORK_ERROR,
                GenieGeminiWebTransport.SessionIssue.NETWORK_TIMEOUT,
                GenieGeminiWebTransport.SessionIssue.ACCESS_TEMPORARILY_UNAVAILABLE}) {
            if (!issue.retryableRefresh()) throw new AssertionError("Transient refresh must retry: " + issue);
        }
        for (var issue : new GenieGeminiWebTransport.SessionIssue[]{
                GenieGeminiWebTransport.SessionIssue.UNAUTHENTICATED,
                GenieGeminiWebTransport.SessionIssue.TERMS_REQUIRED,
                GenieGeminiWebTransport.SessionIssue.ACCOUNT_UNTRUSTED,
                GenieGeminiWebTransport.SessionIssue.DEPENDENCY_UNAVAILABLE}) {
            if (issue.retryableRefresh()) throw new AssertionError("User action must not be retried as a network failure: " + issue);
        }
        System.out.println("GENIE_SESSION_CHECKS_PASS network retries and actual authorization failures remain distinct");
    }
}

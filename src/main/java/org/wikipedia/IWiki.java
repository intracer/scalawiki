package org.wikipedia;

import javax.security.auth.login.CredentialException;
import javax.security.auth.login.CredentialNotFoundException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public interface IWiki {
    String getDomain();

    int getThrottle();

    void setThrottle(int throttle);

    String getScriptPath() throws IOException;

    void setUserAgent(String useragent);

    String getUserAgent();

    void setUsingCompressedRequests(boolean zipped);

    boolean isUsingCompressedRequests();

    boolean isResolvingRedirects();

    void setResolveRedirects(boolean b);

    void setMarkBot(boolean markbot);

    boolean isMarkBot();

    void setMarkMinor(boolean minor);

    boolean isMarkMinor();

    int getMaxLag();

    void setMaxLag(int lag);

    int getAssertionMode();

    void setAssertionMode(int mode);

    int getStatusCheckInterval();

    void setStatusCheckInterval(int interval);

    void login(String username, char[] password) throws IOException, FailedLoginException;

    //Enables login while using a string password
    void login(String username, String password) throws IOException, FailedLoginException;

    void logout();

    void logoutServerSide() throws IOException;

    boolean hasNewMessages() throws IOException;

    int getCurrentDatabaseLag() throws IOException;

    HashMap<String, Integer> getSiteStatistics() throws IOException;

    String version() throws IOException;

    String parse(String markup) throws IOException;

    String random() throws IOException;

    String random(int... ns) throws IOException;

    String getTalkPage(String title) throws IOException;

    HashMap getPageInfo(String page) throws IOException;

    HashMap[] getPageInfo(String[] pages) throws IOException;

    int namespace(String title) throws IOException;

    int[] namespace(String[] titles) throws IOException;

    String namespaceIdentifier(int namespace) throws IOException;

    HashMap<String, Integer> getNamespaces() throws IOException;

    boolean[] exists(String... titles) throws IOException;

    String getPageText(String title) throws IOException;

    List<Wiki.Revision> getPagesText(String... titles) throws IOException;

    String getSectionText(String title, int number) throws IOException;

    String getRenderedText(String title) throws IOException;

    void edit(String title, String text, String summary) throws IOException, LoginException;

    void edit(String title, String text, String summary, Calendar basetime) throws IOException, LoginException;

    void edit(String title, String text, String summary, int section) throws IOException, LoginException;

    void edit(String title, String text, String summary, int section, Calendar basetime)
            throws IOException, LoginException;

    void edit(String title, String text, String summary, boolean minor, boolean bot,
              int section, Calendar basetime) throws IOException, LoginException;

    void newSection(String title, String subject, String text, boolean minor, boolean bot) throws IOException, LoginException;

    void prepend(String title, String stuff, String summary, boolean minor, boolean bot) throws IOException, LoginException;

    void delete(String title, String reason) throws IOException, LoginException;

    void purge(boolean links, String... titles) throws IOException;

    String[] getImagesOnPage(String title) throws IOException;

    String[] getCategories(String title) throws IOException;

    String[] getTemplates(String title, int... ns) throws IOException;

    HashMap<String, String> getInterWikiLinks(String title) throws IOException;

    String[] getLinksOnPage(String title) throws IOException;

    LinkedHashMap<String, String> getSectionMap(String page) throws IOException;

    Wiki.Revision getTopRevision(String title) throws IOException;

    Wiki.Revision getFirstRevision(String title) throws IOException;

    Wiki.Revision[] getPageHistory(String title) throws IOException;

    Wiki.Revision[] getPageHistory(String title, Calendar start, Calendar end) throws IOException;

    void move(String title, String newTitle, String reason) throws IOException, LoginException;

    void move(String title, String newTitle, String reason, boolean noredirect, boolean movetalk,
              boolean movesubpages) throws IOException, LoginException;

    void protect(String page, HashMap<String, Object> protectionstate) throws IOException, CredentialException;

    void unprotect(String page) throws IOException, CredentialException;

    String export(String title) throws IOException;

    Wiki.Revision getRevision(long oldid) throws IOException;

    void rollback(Wiki.Revision revision) throws IOException, LoginException;

    void rollback(Wiki.Revision revision, boolean bot, String reason) throws IOException, LoginException;

    void undo(Wiki.Revision rev, Wiki.Revision to, String reason, boolean minor,
              boolean bot) throws IOException, LoginException;

    byte[] getImage(String title) throws IOException;

    byte[] getImage(String title, int width, int height) throws IOException;

    HashMap<String, Object> getFileMetadata(String file) throws IOException;

    String[] getDuplicates(String file) throws IOException;

    Wiki.LogEntry[] getImageHistory(String title) throws IOException;

    byte[] getOldImage(Wiki.LogEntry entry) throws IOException;

    Wiki.LogEntry[] getUploads(Wiki.User user) throws IOException;

    Wiki.LogEntry[] getUploads(Wiki.User user, Calendar start, Calendar end) throws IOException;

    void upload(File file, String filename, String contents, String reason) throws IOException, LoginException;

    boolean userExists(String username) throws IOException;

    String[] allUsers(String start, int number) throws IOException;

    String[] allUsersWithPrefix(String prefix) throws IOException;

    String[] allUsers(String start, int number, String prefix) throws IOException;

    Wiki.User getUser(String username) throws IOException;

    Wiki.User getCurrentUser();

    Wiki.Revision[] contribs(String user, int... ns) throws IOException;

    @Deprecated
    Wiki.Revision[] rangeContribs(String range) throws IOException;

    Wiki.Revision[] contribs(String user, String prefix, Calendar end, Calendar start, int... ns) throws IOException;

    void emailUser(Wiki.User user, String message, String subject, boolean emailme) throws IOException, LoginException;

    void watch(String... titles) throws IOException, CredentialNotFoundException;

    void unwatch(String... titles) throws IOException, CredentialNotFoundException;

    String[] getRawWatchlist() throws IOException, CredentialNotFoundException;

    String[] getRawWatchlist(boolean cache) throws IOException, CredentialNotFoundException;

    boolean isWatched(String title) throws IOException, CredentialNotFoundException;

    Wiki.Revision[] watchlist() throws IOException, CredentialNotFoundException;

    Wiki.Revision[] watchlist(boolean allrev, int... ns) throws IOException, CredentialNotFoundException;

    String[][] search(String search, int... namespaces) throws IOException;

    String[] imageUsage(String image, int... ns) throws IOException;

    String[] whatLinksHere(String title, int... ns) throws IOException;

    String[] whatLinksHere(String title, boolean redirects, int... ns) throws IOException;

    Wiki.Page[] whatTranscludesHere(String title, int... ns) throws IOException;

    String[] getCategoryMembers(String name, int... ns) throws IOException;

    String[] getCategoryMembers(String name, boolean subcat, int... ns) throws IOException;

    ArrayList[] linksearch(String pattern) throws IOException;

    ArrayList[] linksearch(String pattern, String protocol, int... ns) throws IOException;

    Wiki.LogEntry[] getIPBlockList(String user) throws IOException;

    Wiki.LogEntry[] getIPBlockList(Calendar start, Calendar end) throws IOException;

    Wiki.LogEntry[] getLogEntries(int amount) throws IOException;

    Wiki.LogEntry[] getLogEntries(Wiki.User user) throws IOException;

    Wiki.LogEntry[] getLogEntries(String target) throws IOException;

    Wiki.LogEntry[] getLogEntries(Calendar start, Calendar end) throws IOException;

    Wiki.LogEntry[] getLogEntries(int amount, String type, String action) throws IOException;

    Wiki.LogEntry[] getLogEntries(Calendar start, Calendar end, int amount, String log, String action,
                                  Wiki.User user, String target, int namespace) throws IOException;

    String[] prefixIndex(String prefix) throws IOException;

    String[] shortPages(int cutoff) throws IOException;

    String[] shortPages(int cutoff, int namespace) throws IOException;

    String[] longPages(int cutoff) throws IOException;

    String[] longPages(int cutoff, int namespace) throws IOException;

    String[] listPages(String prefix, HashMap<String, Object> protectionstate, int namespace) throws IOException;

    String[] listPages(String prefix, HashMap<String, Object> protectionstate, int namespace, int minimum,
                       int maximum) throws IOException;

    String[] queryPage(String page) throws IOException, CredentialNotFoundException;

    Wiki.Revision[] newPages(int amount) throws IOException;

    Wiki.Revision[] newPages(int amount, int rcoptions) throws IOException;

    Wiki.Revision[] newPages(int amount, int rcoptions, int... ns) throws IOException;

    Wiki.Revision[] recentChanges(int amount) throws IOException;

    Wiki.Revision[] recentChanges(int amount, int... ns) throws IOException;

    Wiki.Revision[] recentChanges(int amount, int rcoptions, int... ns) throws IOException;

    String[][] getInterWikiBacklinks(String prefix) throws IOException;

    String[][] getInterWikiBacklinks(String prefix, String title) throws IOException;

    Wiki.Page createPage(String title);

    String normalize(String s);

    @Deprecated
    void setLogLevel(Level level);

    Calendar makeCalendar();
}

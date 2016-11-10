package genj.view;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.javascript.host.URL;

public class FacebookTokenizer {
	private static final String API_KEY = "1345837392094044"; 
										 //"336614853397611";
	private static final String REDIRECT_URL =  "https://www.facebook.com/connect/login_success.html";
												// "https%3A%2F%2Fwww.facebook.com%2Fconnect%2Flogin_success.html%3Fdisplay%3Dpage";
	
	private static final String LOGIN_FORM_ID = "login_form";
	private static final String INPUT_EMAIL_NAME = "email";
	private static final String INPUT_PASSWORD_NAME = "pass";
	private static final String INPUT_LOGIN_NAME = "login";
	
	private static final String ACCESS_FORM_ID = "platformDialogForm"; 
			// "uiserver_form";	
	private static final String INPUT_ALLOW_NAME = "__CONFIRM__";
			// "grant_clicked";
	
	private static final String GRAPH_URL = "https://graph.facebook.com/";
	
	private WebClient webClient;
	private String accessToken;
	private String code;
	
	
	public FacebookTokenizer() {
		initWebClient();
	}
	
	public void login(String email, String password) {
		try {
			HtmlForm loginForm = getForm(LOGIN_FORM_ID);
			if (loginForm != null) {
				loginForm.getInputByName(INPUT_EMAIL_NAME).setValueAttribute(email);
				loginForm.getInputByName(INPUT_PASSWORD_NAME).setValueAttribute(password);
				// HtmlElement button = loginForm.getOneHtmlElementByAttribute("button", "name", INPUT_LOGIN_NAME);				
				HtmlButton button = loginForm.getButtonByName(INPUT_LOGIN_NAME);
				HtmlPage page = null;
				if (button != null)
					page = button.click();
								
				HtmlForm accessForm = null;			
				accessForm = getForm(page, ACCESS_FORM_ID);				
				if (accessForm != null) {
					page = accessForm.getInputByName(INPUT_ALLOW_NAME).click();
				}
				if (page.getBody().getTextContent().toLowerCase().contains("success")) {
					getAccessToken(page.getUrl());
					// getCode(page.getUrl());
				}
			}
		} catch (Exception e) {
			throw new NullPointerException();
		}
	}
	
	public String getAccessToken() {
		return this.accessToken;
	}
	
	public String getUserInfo(String id) {
		String requestURL = GRAPH_URL + id + "?access_token=" + accessToken;
		return sendRequest(requestURL);
	}
	
	public String writeInWall(String id, String msg) {
		String requestURL = GRAPH_URL + id + "?access_token=" + accessToken + "&message=" + encodeMsg(msg);
		return publish(requestURL);
	}
	
	private void initWebClient() {
		webClient = new WebClient();
		webClient.getOptions().setCssEnabled(false);
		webClient.getOptions().setJavaScriptEnabled(false);
	}
	
	private HtmlForm getForm(String formID) {
		HtmlForm form = null;
		
		try {
			HtmlPage page = webClient.getPage(getLoginURL());
			form = getForm(page, formID);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return form;
	}
	
	private HtmlForm getForm(HtmlPage page, String formID) {
		HtmlForm form = null;
		
		List<HtmlForm> forms = page.getForms();
		
		for (HtmlForm tForm : forms) {
			String id = tForm.getId();
			if (id.equalsIgnoreCase(formID)) {
				form = tForm;
				break;
			}
		}
		
		return form;
	}
	
	
	private void getAccessToken(java.net.URL link) {
		String ref = link.getRef();
		if (ref == null)
			throw new NullPointerException();
		String token = "access_token=";
		String expires = "&expires";
		
		int startIndex = ref.indexOf(token);
		int endIndex = ref.indexOf(expires);
		
		if (startIndex != -1 && endIndex != -1) {
			accessToken = ref.substring(startIndex + token.length(), endIndex);
		}
	}
	
	private void getCode(java.net.URL link) {
		String ref = link.getFile();
		String code = "code=";
		
		int startIndex = ref.indexOf(code);
		int endIndex = ref.length();
		
		if (startIndex != -1 && endIndex != -1) {
			code = ref.substring(startIndex + code.length(), endIndex);
		}
	}
	
	private String sendRequest(String request) {
		String result = null;
		
		try {
			HttpGet get = new HttpGet(request);
			DefaultHttpClient client = new DefaultHttpClient();
			HttpResponse response = client.execute(get);
			result = EntityUtils.toString(response.getEntity());
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	private String publish(String request) {
		String result = null;
		
		try {
			HttpPost post = new HttpPost(request);
			DefaultHttpClient client = new DefaultHttpClient();
			HttpResponse response = client.execute(post);
			result = EntityUtils.toString(response.getEntity());
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	private String encodeMsg(String msg) {
		try {
			msg = URLEncoder.encode(msg, HTTP.UTF_8);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return msg;
	}
	
	private String getLoginURL() {
		String s = "https://www.facebook.com/dialog/oauth?client_id=" + API_KEY + "&redirect_uri=" + REDIRECT_URL + "&scope=" + getPermissions() + "&response_type=token";
//		String s = "https://www.facebook.com/dialog/permissions.request?_path=permissions.request&app_id=" + API_KEY 
//				 + "&redirect_uri=" + REDIRECT_URL 
//				 + "&response_type=token&perms=" + getPermissions();

		return s;
	}
	
	private String getPermissions() {
		StringBuilder permissions = new StringBuilder();
		List<String> permissionList = new ArrayList<>(); 
		
//		permissionList.add("public_profile");
//		permissionList.add("user_friends");
//		permissionList.add("email");
//		permissionList.add("user_about_me");
//		permissionList.add("user_actions.books");
//		permissionList.add("user_actions.fitness");
//		permissionList.add("user_actions.music");
//		permissionList.add("user_actions.news");
//		permissionList.add("user_actions.video");
//		permissionList.add("user_actions:{app_namespace}");
//		permissionList.add("user_birthday");
//		permissionList.add("user_education_history");
//		permissionList.add("user_events");
//		permissionList.add("user_games_activity");
//		permissionList.add("user_hometown");
//		permissionList.add("user_likes");
//		permissionList.add("user_location");
//		permissionList.add("user_managed_groups");
//		permissionList.add("user_photos");
//		permissionList.add("user_posts");
//		permissionList.add("user_relationships");
//		permissionList.add("user_relationship_details");
//		permissionList.add("user_religion_politics");
//		permissionList.add("user_tagged_places");
//		permissionList.add("user_videos");
//		permissionList.add("user_website");
//		permissionList.add("user_work_history");
//		permissionList.add("read_custom_friendlists");
//		permissionList.add("read_insights");
//		permissionList.add("read_audience_network_insights");
//		permissionList.add("read_page_mailboxes");
//		permissionList.add("manage_pages");
//		permissionList.add("publish_pages");
		permissionList.add("publish_actions");
//		permissionList.add("rsvp_event");
//		permissionList.add("pages_show_list");
//		permissionList.add("pages_manage_cta");
//		permissionList.add("pages_manage_instant_articles");
//		permissionList.add("ads_read");
//		permissionList.add("ads_management");
//		permissionList.add("business_management");
//		permissionList.add("pages_messaging");
//		permissionList.add("pages_messaging_phone_number");
		
		for (int i = 0; i < permissionList.size(); i++) {
			permissions.append(permissionList.get(i));
			if (i < permissionList.size() - 1) 
				permissions.append(",");
		}
	
		return permissions.toString();
	}
}
package com.nao20010128nao.Wisecraft.misc.compat;
import android.support.v7.app.ActionBarActivity;
import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public abstract class CompatWebViewActivity extends SimpleCompatActivity
{
	WebView webView;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		setContentView(R.layout.webview_activity_compat);
		webView=(WebView)findViewById(R.id.webview);
		webView.setWebViewClient(new WebViewClient(){});
		webView.getSettings().setJavaScriptEnabled(true);
	}
	protected WebView getWebView() {
		return webView;
	}
	public void loadUrl(String url){
		webView.loadUrl(url);
	}
	public void reload(){
		webView.reload();
	}
}

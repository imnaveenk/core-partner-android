package com.getmeashop.realestate.partner.util;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import com.getmeashop.realestate.partner.Callbacks;
import com.getmeashop.realestate.partner.LoginActivity;
import com.getmeashop.realestate.partner.PersistentCookieStore;
import com.getmeashop.realestate.partner.Utils;
import com.getmeashop.realestate.partner.database.DatabaseHandler;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import java.io.FileNotFoundException;
import java.net.SocketException;
import java.util.List;

/**
 * handles post request
 */
public class PostRequest {
    Boolean success = true;
    SharedPreferences sp;
    SharedPreferences.Editor editor;

    /**
     * constructor to get url and callback object and context
     * <p/>
     * hits a get request to url and calls callback functions
     *
     * @param url      Where request is to be made
     * @param callback object of callback
     */
    public PostRequest(final Callbacks callback, final String url, final Context context) {

        sp = context.getSharedPreferences("Users",
                Context.MODE_PRIVATE);
        editor = sp.edit();
        class postrequest extends AsyncTask<String, String, Integer> {

            @Override
            protected Integer doInBackground(String... arg0) {
                int statusCode = 0;
                try {
                    HttpParams httpParameters = new BasicHttpParams();
                    BasicCookieStore cookieStore2 = new BasicCookieStore();
                    PersistentCookieStore cookieStore = new PersistentCookieStore(context);
                    cookieStore2.addCookie(cookieStore.getCookies().get(0));
                    // cookieStore2.addCookie(loginCookies.get(1));
                    HttpContext localContext = new BasicHttpContext();
                    localContext.setAttribute(ClientContext.COOKIE_STORE,
                            cookieStore2);
                    SharedPreferences sp;
                    sp = context.getSharedPreferences("Users", Context.MODE_PRIVATE);
                    String username = sp.getString("userName", "");
                    // Setup timeouts
                    HttpConnectionParams.setConnectionTimeout(httpParameters,
                            10000);
                    if (!username.equalsIgnoreCase("") &&
                            (url.contains("mobile/" + username + "/product/") || url.contains("mobile/" + username + "/category/"))) {
                        HttpConnectionParams.setSoTimeout(httpParameters, 45000);
                    } else {
                        HttpConnectionParams.setSoTimeout(httpParameters, 15000);
                    }

                    HttpClient httpclients = new DefaultHttpClient(
                            httpParameters);

                    HttpPost httpPost = new HttpPost(url);
                    httpPost.setHeader("Referer", url);
                    httpPost.setHeader("X-Requested-From", "partner-app");
                    httpPost.setHeader("X-CSRFToken", cookieStore.getCookies()
                            .get(0).getValue());

                    try {
                        if(sp.getString("sessionid", "").length() !=0)
                            httpPost.addHeader("Cookie",
                                    sp.getString("sessionid", "").substring(0, sp.getString("sessionid", "").indexOf("; ") +1 ));
                        else
                            httpPost.addHeader("Cookie", cookieStore.getCookies().get(0).getName() + "=" + cookieStore.getCookies().get(0).getValue() + ";");
                    } catch (Exception e) {

                    }

                    Log.e("url", url);
                    HttpPost httpPost1 = callback
                            .preparePostData(url, httpPost);
                    if (httpPost1 != null) {
                        httpPost = httpPost1;
                    }
                    HttpResponse response = httpclients.execute(httpPost,
                            localContext);
                    //Added
                    List<Cookie> cookies = cookieStore2.getCookies();
                    for (Cookie cookie : cookies) {
                        if (cookie.getName().equals("csrftoken")) {
                            cookieStore.clear();
                            cookieStore.addCookie(cookie);
                        }
                    }
                    statusCode = response.getStatusLine().getStatusCode();
                    try {
                        if (url.contains("login")) {
                            Header[] head = response.getHeaders("Set-Cookie");
                            if (head != null) {
                                for (int i = 0; i < head.length; i++) {
                                    if (head[i].getValue().contains("sessionid")) {
                                        editor.putString("sessionid",
                                                head[i].getValue());
                                        editor.commit();
                                        break;
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                    }

                    callback.processResponse(response, url);

                } catch (ClientProtocolException e) {
                    statusCode = GetRequest.CLIENT_PROTOCOL_ERROR;
                    success = false;
                    e.printStackTrace();
                } catch (SocketException e) {
                    statusCode = GetRequest.SOCKET_ERROR;
                    success = false;
                    e.printStackTrace();
                } catch (ConnectTimeoutException e) {
                    success = false;
                    e.printStackTrace();
                    statusCode = GetRequest.IO_ERROR;
                    success = false;
                    e.printStackTrace();
                } catch (FileNotFoundException e) {
                    statusCode = GetRequest.FILE_NOT_FOUND;
                    success = false;
                    e.printStackTrace();
                } catch (Exception e) {
                    success = false;
                    e.printStackTrace();
                } finally {
                    return statusCode;
                }
            }

            @Override
            protected void onPreExecute() {
                callback.preexecute(url);
                super.onPreExecute();
            }
/*
            @Override
            protected void onProgressUpdate(String... values) {
               // super.onProgressUpdate(values);
                Log.i("makemachine", "onProgressUpdate(): " + String.valueOf(values[0]));
                callback.preexecute(url, (Integer.parseInt(values[0]))*2);
            }
            */

            @Override
            protected void onPostExecute(Integer statusCode) {
                if (statusCode == 401) {
                    Utils.showToast("Your Session is expired, please Login again", context);
                    Intent to_reauthorize = new Intent(context, LoginActivity.class);
                    editor.clear();
                    editor.commit();
                    DatabaseHandler dbh = new DatabaseHandler(context);
                    dbh.deletedatabase();
                    to_reauthorize.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    to_reauthorize.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                        to_reauthorize.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    context.startActivity(to_reauthorize);

                } else if (success)
                    callback.postexecute(url, statusCode);
                else {
                    callback.postexecute("failed", statusCode);
                }
            }
        }
        if (Utils.isConnectingToInternet(context))
            new postrequest().execute();
    }

}

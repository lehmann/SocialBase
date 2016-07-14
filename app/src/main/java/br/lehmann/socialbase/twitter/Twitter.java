package br.lehmann.socialbase.twitter;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.Base64;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.SyncHttpClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Future;

import br.lehmann.socialbase.ItemListActivity;
import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.HttpStatus;

/**
 * Classe que concentra todas as rotinas que interajam com os Web services REST fornecidos pelo Twitter
 * Created by André on 06/07/2016.
 */
public class Twitter {

    private static final String CONSUMER_KEY = "bZnv4rwP324RZRgzIaTuTQ08U";
    private static final String CONSUMER_SECRET = "QRlJwjPEQVJgMEs5A1EVAMSpcTL8tigh5yjZTNZOqgPMOq41yx";

    public static final String TWEETS = "SocialBase.TWEETS";
    public static final String ERROR = "SocialBase.ERROR";
    public static final String TRENDING_TOPICS = "SocialBase.TRENDING_TOPICS";

    public static void search(String tt, final Handler onSuccess, final Handler onFailure) {
        AsyncTask<String, Void, ArrayList<Tweet>> task = new AsyncTask<String, Void, ArrayList<Tweet>>() {

            private String errorMsg;

            @Override
            protected ArrayList<Tweet> doInBackground(final String... query) {
                final SyncHttpClient httpClient = new SyncHttpClient();
                final ArrayList<Tweet> ret = new ArrayList<Tweet>();

                try {
                    RequestParams requestParams = new RequestParams();
                    requestParams.put("grant_type", "client_credentials");
                    httpClient.addHeader("Authorization", "Basic " + Base64.encodeToString((CONSUMER_KEY + ":" + CONSUMER_SECRET).getBytes(), Base64.NO_WRAP));
                    httpClient.addHeader("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");

                    httpClient.post("https://api.twitter.com/oauth2/token", requestParams, new JsonHttpResponseHandler() {

                        @Override
                        public void onSuccess(int statusCode, Header[] headers, JSONObject jsonObject) {
                            try {
                                httpClient.addHeader("Authorization", jsonObject.getString("token_type") + " " + jsonObject.getString("access_token"));
                                RequestParams params = new RequestParams();
                                params.put("q", query[0]);
                                httpClient.get("https://api.twitter.com/1.1/search/tweets.json", params, new JsonHttpResponseHandler() {
                                            @Override
                                            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                                                try {
                                                    JSONArray tweets = response.getJSONArray("statuses");
                                                    for (int i = 0; i < tweets.length(); i++) {
                                                        JSONObject tweet = tweets.getJSONObject(i);
                                                        ret.add(new Tweet(tweet.getString("text"), query[0]));
                                                    }
                                                } catch (JSONException e) {
                                                    throw new RuntimeException(e);
                                                }
                                            }

                                            @Override
                                            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                                                throw new RuntimeException(responseString, throwable);
                                            }

                                            @Override
                                            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                                                throw new RuntimeException(throwable);
                                            }

                                            @Override
                                            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
                                                throw new RuntimeException(throwable);
                                            }
                                        }
                                );
                            } catch (JSONException e) {
                                throw new RuntimeException(e);
                            }
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                            throw new RuntimeException(responseString, throwable);
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                            throw new RuntimeException(throwable);
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
                            throw new RuntimeException(throwable);
                        }
                    });
                } catch (Exception e) {
                    this.errorMsg = e.getMessage();
                    this.cancel(true);
                }
                return ret;
            }

            @Override
            protected void onPostExecute(ArrayList<Tweet> tweets) {
                if (onSuccess != null) {
                    Message onSuccessMsg = Message.obtain(onSuccess);
                    Bundle successBundle = new Bundle();
                    successBundle.putSerializable(Twitter.TWEETS, tweets);
                    onSuccessMsg.setData(successBundle);
                    onSuccessMsg.setAsynchronous(true);
                    onSuccessMsg.sendToTarget();
                }
            }

            @Override
            protected void onCancelled() {
                if (onFailure != null) {
                    Message onSuccessMsg = Message.obtain(onFailure);
                    Bundle successBundle = new Bundle();
                    successBundle.putString(Twitter.ERROR, errorMsg != null ? errorMsg : "Erro ao buscar os tweets");
                    onSuccessMsg.setData(successBundle);
                    onSuccessMsg.setAsynchronous(true);
                    onSuccessMsg.sendToTarget();
                }
            }
        };
        task.execute(tt);
    }

    public static void loadingTrendingTopics(final Handler onSuccess, final Handler onFailure) {
        final AsyncTask<Void, Void, ArrayList<TrendingTopic>> task = new AsyncTask<Void, Void, ArrayList<TrendingTopic>>() {

            private String errorMsg;

            @Override
            protected ArrayList<TrendingTopic> doInBackground(Void... params) {
                final SyncHttpClient httpClient = new SyncHttpClient();
                final ArrayList<TrendingTopic> ret = new ArrayList<TrendingTopic>();

                try {
                    RequestParams requestParams = new RequestParams();
                    requestParams.put("grant_type", "client_credentials");
                    httpClient.addHeader("Authorization", "Basic " + Base64.encodeToString((CONSUMER_KEY + ":" + CONSUMER_SECRET).getBytes(), Base64.NO_WRAP));
                    httpClient.addHeader("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
                    httpClient.post("https://api.twitter.com/oauth2/token", requestParams, new JsonHttpResponseHandler() {

                        @Override
                        public void onSuccess(int statusCode, Header[] headers, JSONObject jsonObject) {
                            try {
                                httpClient.addHeader("Authorization", jsonObject.getString("token_type") + " " + jsonObject.getString("access_token"));
                                RequestParams params = new RequestParams();
                                params.put("id", "1");
                                httpClient.get("https://api.twitter.com/1.1/trends/place.json", params, new JsonHttpResponseHandler() {
                                            @Override
                                            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                                                try {
                                                    JSONObject obj = response.getJSONObject(0);
                                                    JSONArray trends = obj.getJSONArray("trends");
                                                    for (int i = 0; i < trends.length(); i++) {
                                                        JSONObject trend = trends.getJSONObject(i);
                                                        if (trend.optInt("tweet_volume") > 0) {
                                                            ret.add(new TrendingTopic(trend.optInt("tweet_volume"), trend.getString("name"), trend.getString("query")));
                                                        }
                                                    }
                                                } catch (JSONException e) {
                                                    throw new RuntimeException(e);
                                                }
                                            }

                                            @Override
                                            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                                                throw new RuntimeException(responseString, throwable);
                                            }

                                            @Override
                                            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                                                throw new RuntimeException(throwable);
                                            }

                                            @Override
                                            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
                                                throw new RuntimeException(throwable);
                                            }
                                        }
                                );
                            } catch (JSONException e) {
                                throw new RuntimeException(e);
                            }
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                            throw new RuntimeException(responseString, throwable);
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                            throw new RuntimeException(throwable);
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
                            throw new RuntimeException(throwable);
                        }
                    });
                    Collections.sort(ret, new Comparator<TrendingTopic>() {
                        @Override
                        public int compare(TrendingTopic lhs, TrendingTopic rhs) {
                            return rhs.getVolume() - lhs.getVolume();
                        }
                    });
                } catch (Exception e) {
                    this.errorMsg = e.getMessage();
                    this.cancel(true);
                }
                return ret;
            }

            @Override
            protected void onPostExecute(ArrayList<TrendingTopic> trendingTopics) {
                if (onSuccess != null) {
                    Message onSuccessMsg = Message.obtain(onSuccess);
                    Bundle successBundle = new Bundle();
                    successBundle.putSerializable(Twitter.TRENDING_TOPICS, trendingTopics);
                    onSuccessMsg.setData(successBundle);
                    onSuccessMsg.setAsynchronous(true);
                    onSuccessMsg.sendToTarget();
                }
            }

            @Override
            protected void onCancelled() {
                if (onFailure != null) {
                    Message onFailureMsg = Message.obtain(onFailure);
                    Bundle failureBundle = new Bundle();
                    failureBundle.putString(Twitter.ERROR, errorMsg != null ? errorMsg : "Erro ao buscar os tweets");
                    onFailureMsg.setData(failureBundle);
                    onFailureMsg.setAsynchronous(true);
                    onFailureMsg.sendToTarget();
                }
            }
        };
        task.execute();
    }
}

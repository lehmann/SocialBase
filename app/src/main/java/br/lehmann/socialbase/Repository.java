package br.lehmann.socialbase;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.github.rahatarmanahmed.cpv.CircularProgressView;

import java.util.ArrayList;
import java.util.List;

import br.lehmann.socialbase.db.Persister;
import br.lehmann.socialbase.twitter.TrendingTopic;
import br.lehmann.socialbase.twitter.Tweet;
import br.lehmann.socialbase.twitter.Twitter;

/**
 * Faz a carga de todos os dados envolvidos no aplicativo e trata da métrica de cache para cada entidade envolvida
 * Created by André on 08/07/2016.
 */

public class Repository {

    public static final String PREFERENCES_CACHE_TIMEOUT = "CacheTimeout";
    public static final String PREFERENCES_LAST_CACHE_MISS = "LastCacheMiss";

    private Context ctx;

    public Repository(Context ctx) {
        this.ctx = ctx;
    }

    /**
     * Realiza a carga dos Trendgins Topic globais do Twitter e informa assincronamente o sucesso ou fracasso da carga de TTs.
     * Os TTs podem ou não virem do cache de dados, que são armazenados no SQLite a cada vez que é realizada a chamada do Web service.
     * A métrica de cache é de manter os dados por até 1 minuto, quando que os TTs são removidos, é feita nova carga no Web service e os novos TTs são armazenados no SQLite.
     * @param fromCache indica se os TTs podem ou não virem do cache
     * @param onSuccess {@link Handler handler} que será invocado assincronamente quando os TTs forem carregados com sucesso
     * @param onFailure {@link Handler handler} que será invocado caso haja alguma erro durante a carga dos TTs
     */
    public void loadTrendingTopics(boolean fromCache, final Handler onSuccess, final Handler onFailure) {
        SharedPreferences cacheMissPref = ctx.getSharedPreferences(TrendingTopic.class.getName(), Context.MODE_PRIVATE);
        long cacheTimeout = cacheMissPref.getLong(PREFERENCES_CACHE_TIMEOUT, (1000 * 60));
        long cacheMiss = cacheMissPref.getLong(PREFERENCES_LAST_CACHE_MISS, cacheTimeout + 1);
        if ((System.currentTimeMillis() - cacheMiss) > cacheTimeout) {
            Persister db = Persister.getInstance(ctx);
            db.removeTrendings();
            fromCache = Boolean.FALSE;
            SharedPreferences.Editor editor = cacheMissPref.edit();
            editor.putLong(PREFERENCES_LAST_CACHE_MISS, System.currentTimeMillis());
            editor.commit();
        }
        if (fromCache) {
            Persister db = Persister.getInstance(ctx);
            ArrayList<TrendingTopic> ret = db.read();
            if (!ret.isEmpty()) {
                Message onSuccessMsg = Message.obtain(onSuccess);
                Bundle successBundle = new Bundle();
                successBundle.putSerializable(Twitter.TRENDING_TOPICS, ret);
                onSuccessMsg.setData(successBundle);
                onSuccessMsg.setAsynchronous(true);
                onSuccessMsg.sendToTarget();
                return;
            }
        }
        Twitter.loadingTrendingTopics(new Handler() {
            @Override
            public void handleMessage(Message msg) {
                List<TrendingTopic> ret = (List<TrendingTopic>) msg.getData().getSerializable(Twitter.TRENDING_TOPICS);
                Persister db = Persister.getInstance(ctx);
                db.removeTrendings();
                db.insertTT(ret);
                onSuccess.handleMessage(msg);
            }
        }, new Handler() {
            @Override
            public void handleMessage(Message msg) {
                onFailure.handleMessage(msg);
            }
        });
    }

    /**
     * Realiza a carga de tweets como resultado da String de query informada por parâmetro.
     * Os tweets podem ou não virem do cache de dados, que são armazenados no SQLite a cada vez que é realizada a chamada do Web service.
     * A métrica de cache é de manter os tweets por até 1 minuto, quando todos os tweets são removidos, é feita nova carga no Web service e
     *      os novos tweets da query pesquisada são armazenados no SQLite.
     * @param fromCache indica se os tweets podem ou não virem do cache
     * @param onSuccess {@link Handler handler} que será invocado assincronamente quando os tweets forem carregados com sucesso
     * @param onFailure {@link Handler handler} que será invocado caso haja alguma erro durante a carga dos tweets
     */
    public void loadTweets(String query, boolean fromCache, final Handler onSuccess, final Handler onFailure) {
        SharedPreferences cacheMissPref = ctx.getSharedPreferences(Tweet.class.getName(), Context.MODE_PRIVATE);
        long cacheTimeout = cacheMissPref.getLong(PREFERENCES_CACHE_TIMEOUT, (1000 * 60));
        long cacheMiss = cacheMissPref.getLong(PREFERENCES_LAST_CACHE_MISS, cacheTimeout + 1);
        if((System.currentTimeMillis() - cacheMiss) > cacheTimeout) {
            final Persister db = Persister.getInstance(ctx);
            db.removeTweets();
            fromCache = Boolean.FALSE;
            SharedPreferences.Editor edit = cacheMissPref.edit();
            edit.putLong(PREFERENCES_LAST_CACHE_MISS, System.currentTimeMillis());
            edit.commit();
        }

        if(fromCache) {
            Persister db = Persister.getInstance(ctx);
            ArrayList<Tweet> tweets = db.read(query);
            if(tweets.isEmpty()) {
                Twitter.search(query, new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        ArrayList<Tweet> tweets = (ArrayList<Tweet>) msg.getData().getSerializable(Twitter.TWEETS);
                        Persister db = Persister.getInstance(ctx);
                        db.insert(tweets);

                        Message onSuccessMsg = Message.obtain(onSuccess);
                        Bundle successBundle = new Bundle();
                        successBundle.putSerializable(Twitter.TWEETS, tweets);
                        onSuccessMsg.setData(successBundle);
                        onSuccessMsg.setAsynchronous(true);
                        onSuccessMsg.sendToTarget();
                    }
                }, new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        Message onFailureMsg = Message.obtain(onFailure);
                        Bundle failureBundle = new Bundle();
                        failureBundle.putString(Twitter.ERROR, msg.getData().getString(Twitter.ERROR));
                        onFailureMsg.setData(failureBundle);
                        onFailureMsg.setAsynchronous(true);
                        onFailureMsg.sendToTarget();
                    }
                });
            } else {
                Message onSuccessMsg = Message.obtain(onSuccess);
                Bundle successBundle = new Bundle();
                successBundle.putSerializable(Twitter.TWEETS, tweets);
                onSuccessMsg.setData(successBundle);
                onSuccessMsg.setAsynchronous(true);
                onSuccessMsg.sendToTarget();
            }
        } else {
            Twitter.search(query, new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    ArrayList<Tweet> tweets = (ArrayList<Tweet>) msg.getData().getSerializable(Twitter.TWEETS);
                    Persister db = Persister.getInstance(ctx);
                    db.removeTweets();
                    db.insert(tweets);

                    Message onSuccessMsg = Message.obtain(onSuccess);
                    Bundle successBundle = new Bundle();
                    successBundle.putSerializable(Twitter.TWEETS, tweets);
                    onSuccessMsg.setData(successBundle);
                    onSuccessMsg.setAsynchronous(true);
                    onSuccessMsg.sendToTarget();
                }
            }, new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    Message onFailureMsg = Message.obtain(onFailure);
                    Bundle failureBundle = new Bundle();
                    failureBundle.putString(Twitter.ERROR, msg.getData().getString(Twitter.ERROR));
                    onFailureMsg.setData(failureBundle);
                    onFailureMsg.setAsynchronous(true);
                    onFailureMsg.sendToTarget();
                }
            });
        }
    }


}

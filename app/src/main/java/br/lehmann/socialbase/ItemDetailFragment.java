package br.lehmann.socialbase;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.CollapsingToolbarLayout;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.rahatarmanahmed.cpv.CircularProgressView;

import java.util.ArrayList;
import java.util.List;

import br.lehmann.socialbase.db.Persister;
import br.lehmann.socialbase.twitter.Tweet;
import br.lehmann.socialbase.twitter.Twitter;

public class ItemDetailFragment extends Fragment {

    public static final String ARG_TT_QUERY = "tt_query";
    public static final String ARG_TT_NAME = "tt_name";
    public static final String ARG_FROM_CACHE = "from_cache";
    private List<Tweet> tweets;

    public ItemDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_TT_QUERY)) {
            Boolean fromCache = getArguments().getBoolean(ARG_FROM_CACHE);
            String query = getArguments().getString(ARG_TT_QUERY);
            new Repository(getContext()).loadTweets(query, fromCache, new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    tweets = (List<Tweet>) msg.getData().getSerializable(Twitter.TWEETS);
                    RecyclerView recyclerView = (RecyclerView) getView().findViewById(R.id.item_detail);
                    assert recyclerView != null;
                    CircularProgressView progressView = (CircularProgressView) getView().findViewById(R.id.progress_tweet_view);
                    progressView.setVisibility(View.GONE);
                    recyclerView.setAdapter(new ItemDetailFragment.SimpleTweetRecyclerViewAdapter(tweets));
                }
            }, new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    //Handle ERROR message
                    CircularProgressView progressView = (CircularProgressView) getView().findViewById(R.id.progress_tweet_view);
                    progressView.setVisibility(View.GONE);
                    Activity activity = ItemDetailFragment.this.getActivity();
                    View coordinatorLayout = activity.findViewById(R.id.detail_layout);
                    Snackbar snackbar = Snackbar
                            .make(coordinatorLayout, msg.getData().getString(Twitter.ERROR), Snackbar.LENGTH_LONG);
                    snackbar.show();
                }
            });

            Activity activity = this.getActivity();
            CollapsingToolbarLayout appBarLayout = (CollapsingToolbarLayout) activity.findViewById(R.id.toolbar_layout);
            if (appBarLayout != null) {
                appBarLayout.setTitle(getArguments().getString(ARG_TT_NAME));
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.item_detail, container, false);

        if(tweets != null) {
            RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.item_detail);
            assert recyclerView != null;
            recyclerView.setAdapter(new ItemDetailFragment.SimpleTweetRecyclerViewAdapter(tweets));
        } else {
            CircularProgressView progressView = (CircularProgressView) rootView.findViewById(R.id.progress_tweet_view);
            progressView.setVisibility(View.VISIBLE);
        }
        return rootView;
    }

    public class SimpleTweetRecyclerViewAdapter
            extends RecyclerView.Adapter<ItemDetailFragment.SimpleTweetRecyclerViewAdapter.ViewHolder> {
        private final List<Tweet> mValues;

        public SimpleTweetRecyclerViewAdapter(List<Tweet> items) {
            mValues = items;
        }

        @Override
        public ItemDetailFragment.SimpleTweetRecyclerViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_list_content, parent, false);
            return new ItemDetailFragment.SimpleTweetRecyclerViewAdapter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ItemDetailFragment.SimpleTweetRecyclerViewAdapter.ViewHolder holder, int position) {
            holder.mItem = mValues.get(position);
            holder.mContentView.setText(mValues.get(position).getText());
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public final View mView;
            public final TextView mContentView;
            public Tweet mItem;

            public ViewHolder(View view) {
                super(view);
                mView = view;
                mContentView = (TextView) view.findViewById(R.id.content);
            }

            @Override
            public String toString() {
                return super.toString() + " '" + mContentView.getText() + "'";
            }
        }
    }
}

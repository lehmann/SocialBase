package br.lehmann.socialbase;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.rahatarmanahmed.cpv.CircularProgressView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import br.lehmann.socialbase.db.Persister;
import br.lehmann.socialbase.twitter.TrendingTopic;
import br.lehmann.socialbase.twitter.Twitter;

/**
 * An activity representing a list of Items. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link ItemDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
public class ItemListActivity extends AppCompatActivity {

    private View mContentView;

    private boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getTitle());

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final RecyclerView recyclerView = (RecyclerView) findViewById(R.id.item_list);
                recyclerView.setAdapter(new SimpleItemRecyclerViewAdapter(Collections.<TrendingTopic>emptyList()));
                CircularProgressView progressView = (CircularProgressView) findViewById(R.id.progress_trendings_view);
                progressView.setVisibility(View.VISIBLE);
                setupRecyclerView(false, new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        CircularProgressView progressView = (CircularProgressView) findViewById(R.id.progress_trendings_view);
                        progressView.setVisibility(View.GONE);
                        List<TrendingTopic> ret = (List<TrendingTopic>) msg.getData().getSerializable(Twitter.TRENDING_TOPICS);
                        recyclerView.setAdapter(new SimpleItemRecyclerViewAdapter(ret));
                    }

                }, new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        //Handle ERROR message
                        CircularProgressView progressView = (CircularProgressView) findViewById(R.id.progress_trendings_view);
                        progressView.setVisibility(View.GONE);
                        View coordinatorLayout = findViewById(R.id.main_view);
                        Snackbar snackbar = Snackbar
                                .make(coordinatorLayout, msg.getData().getString(Twitter.ERROR), Snackbar.LENGTH_LONG);
                        snackbar.show();
                    }

                });
            }
        });

        final RecyclerView recyclerView = (RecyclerView) findViewById(R.id.item_list);
        assert recyclerView != null;
        setupRecyclerView(true, new Handler() {
            @Override
            public void handleMessage(Message msg) {
                CircularProgressView progressView = (CircularProgressView) findViewById(R.id.progress_trendings_view);
                progressView.setVisibility(View.GONE);
                List<TrendingTopic> ret = (List<TrendingTopic>) msg.getData().getSerializable(Twitter.TRENDING_TOPICS);
                recyclerView.setAdapter(new SimpleItemRecyclerViewAdapter(ret));
            }

        }, new Handler() {
            @Override
            public void handleMessage(Message msg) {
                //Handle ERROR message
                CircularProgressView progressView = (CircularProgressView) findViewById(R.id.progress_trendings_view);
                progressView.setVisibility(View.GONE);
                View coordinatorLayout = findViewById(R.id.main_view);
                Snackbar snackbar = Snackbar
                        .make(coordinatorLayout, msg.getData().getString(Twitter.ERROR), Snackbar.LENGTH_LONG);
                snackbar.show();
            }

        });
        if (findViewById(R.id.item_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
        }
    }

    private void setupRecyclerView(boolean fromCache, Handler onSuccess, Handler onFailure) {
        new Repository(getBaseContext()).loadTrendingTopics(fromCache, onSuccess, onFailure);
    }

    public class SimpleItemRecyclerViewAdapter
            extends RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder> {

        private final List<TrendingTopic> mValues;

        public SimpleItemRecyclerViewAdapter(List<TrendingTopic> items) {
            mValues = items;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_list_content, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            holder.mItem = mValues.get(position);
            holder.mIdView.setText(String.valueOf(mValues.get(position).getVolume()));
            holder.mContentView.setText(mValues.get(position).getName());

            holder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mTwoPane) {
                        Bundle arguments = new Bundle();
                        arguments.putString(ItemDetailFragment.ARG_TT_QUERY, holder.mItem.getQuery());
                        arguments.putString(ItemDetailFragment.ARG_TT_NAME, holder.mItem.getName());
                        ItemDetailFragment fragment = new ItemDetailFragment();
                        fragment.setArguments(arguments);
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.item_detail_container, fragment)
                                .commit();
                    } else {
                        Context context = v.getContext();
                        Intent intent = new Intent(context, ItemDetailActivity.class);
                        intent.putExtra(ItemDetailFragment.ARG_TT_NAME, holder.mItem.getName());
                        intent.putExtra(ItemDetailFragment.ARG_TT_QUERY, holder.mItem.getQuery());

                        context.startActivity(intent);
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public final View mView;
            public final TextView mIdView;
            public final TextView mContentView;
            public TrendingTopic mItem;

            public ViewHolder(View view) {
                super(view);
                mView = view;
                mIdView = (TextView) view.findViewById(R.id.id);
                mContentView = (TextView) view.findViewById(R.id.content);
            }

            @Override
            public String toString() {
                return super.toString() + " '" + mContentView.getText() + "'";
            }
        }
    }
}

package com.ferjuarez.photobooth;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.ProgressBar;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private PhotoEntryAdapter mAdapter;
    private ProgressBar progressBar;
    private DatabaseReference mDatabaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupLayout();

    }

    @Override
    public void onStart() {
        super.onStart();
        //progressBar.setVisibility(View.VISIBLE);
        mDatabaseRef = FirebaseDatabase.getInstance().getReference().child("logs");
        mAdapter = new PhotoEntryAdapter(this, mDatabaseRef);
        mRecyclerView.setAdapter(mAdapter);

        // Make sure new events are visible
        mAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeChanged(int positionStart, int itemCount) {
                mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount());
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();

        // Tear down Firebase listeners in adapter
        if (mAdapter != null) {
            mAdapter.cleanup();
            mAdapter = null;
        }
    }

    private void setupLayout() {
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        // populate RecyclerView
        mRecyclerView = (RecyclerView) findViewById(com.ferjuarez.photobooth.R.id.doorbellView);
        LinearLayoutManager layoutManager =
                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(layoutManager);
    }
}

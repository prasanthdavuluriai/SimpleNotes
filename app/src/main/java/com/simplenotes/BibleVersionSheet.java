package com.simplenotes;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.simplenotes.api.BibleDownloader;
import java.util.Collections;
import java.util.List;

public class BibleVersionSheet extends BottomSheetDialogFragment {
    public interface VersionListener {
        void onVersionSelected(BibleVersion version);
    }

    private VersionListener listener;
    private RecyclerView recyclerView;
    private VersionAdapter adapter;
    private List<BibleVersion> versions = Collections.emptyList();

    public void setListener(VersionListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bible_versions, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        recyclerView = view.findViewById(R.id.recyclerViewVersions);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new VersionAdapter();
        recyclerView.setAdapter(adapter);

        loadVersions();
    }

    private void loadVersions() {
        AppExecutors.getInstance().diskIO().execute(() -> {
            List<BibleVersion> list = AppDatabase.getDatabase(getContext()).bibleDao().getAllVersions();
            // If empty, we might need to seed, but assuming main activity handles seeding
            // for now
            // Or better, handle it here to be safe
            if (list.isEmpty()) {
                seedVersions();
                list = AppDatabase.getDatabase(getContext()).bibleDao().getAllVersions();
            }

            List<BibleVersion> finalList = list;
            AppExecutors.getInstance().mainThread().execute(() -> {
                versions = finalList;
                adapter.notifyDataSetChanged();
            });
        });
    }

    private void seedVersions() {
        // Seed initial data
        BibleDao dao = AppDatabase.getDatabase(getContext()).bibleDao();
        dao.insertVersion(new BibleVersion("kjv", "King James Version", false));
        dao.insertVersion(new BibleVersion("web", "World English Bible", false));
        dao.insertVersion(new BibleVersion("asv", "American Standard (1901)", false));
        dao.insertVersion(new BibleVersion("bbe", "Bible in Basic English", false));
        dao.insertVersion(new BibleVersion("cherokee", "Cherokee New Testament", false));
        dao.insertVersion(new BibleVersion("cuv", "Chinese Union Version", false));
        // Add more as needed based on previous map
    }

    private class VersionAdapter extends RecyclerView.Adapter<VersionAdapter.ViewHolder> {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bible_version, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            BibleVersion version = versions.get(position);
            holder.textName.setText(version.getName());

            if (version.isDownloaded()) {
                holder.iconDownloaded.setVisibility(View.VISIBLE);
                holder.itemView.setOnLongClickListener(null); // No long click if already downloaded
            } else {
                holder.iconDownloaded.setVisibility(View.GONE);

                // Long press to download (Only KJV supported for now)
                holder.itemView.setOnLongClickListener(v -> {
                    if ("kjv".equals(version.getId())) {
                        showDownloadOption(v, version);
                    } else {
                        Toast.makeText(getContext(), "Download not available for this version yet", Toast.LENGTH_SHORT)
                                .show();
                    }
                    return true;
                });
            }

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onVersionSelected(version);
                    dismiss();
                }
            });
        }

        @Override
        public int getItemCount() {
            return versions.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            android.widget.TextView textName;
            android.widget.ImageView iconDownloaded;

            ViewHolder(View itemView) {
                super(itemView);
                textName = itemView.findViewById(R.id.textViewName);
                iconDownloaded = itemView.findViewById(R.id.imageViewDownloaded);
            }
        }
    }

    private void showDownloadOption(View anchor, BibleVersion version) {
        PopupMenu popup = new PopupMenu(getContext(), anchor);
        popup.getMenu().add("Download Offline");
        popup.setOnMenuItemClickListener(item -> {
            Toast.makeText(getContext(), "Downloading " + version.getName() + "...", Toast.LENGTH_SHORT).show();
            BibleDownloader.downloadKJV(getContext(), new BibleDownloader.DownloadCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(getContext(), "Download Complete!", Toast.LENGTH_SHORT).show();
                    loadVersions(); // Refresh list to show checkmark
                }

                @Override
                public void onFailure(String error) {
                    Toast.makeText(getContext(), "Download Failed: " + error, Toast.LENGTH_LONG).show();
                }
            });
            return true;
        });
        popup.show();
    }
}

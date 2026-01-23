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
            // Always try to seed new versions if they don't exist
            seedVersions();

            List<BibleVersion> list = AppDatabase.getDatabase(getContext()).bibleDao().getAllVersions();

            List<BibleVersion> finalList = list;
            AppExecutors.getInstance().mainThread().execute(() -> {
                versions = finalList;
                adapter.notifyDataSetChanged();
            });
        });
    }

    private void seedVersions() {
        BibleDao dao = AppDatabase.getDatabase(getContext()).bibleDao();

        // Helper to insert only if missing (to preserve isDownloaded state)
        safeInsert(dao, new BibleVersion("kjv", "King James Version", false));
        safeInsert(dao, new BibleVersion("bbe", "Bible in Basic English", false));
        safeInsert(dao, new BibleVersion("niv", "New International Version", false));
        safeInsert(dao, new BibleVersion("nlt", "New Living Translation", false));

        // New Languages
        safeInsert(dao, new BibleVersion("ar_svd", "Arabic (SVD)", false));
        safeInsert(dao, new BibleVersion("zh_cuv", "Chinese Union Version", false));
        safeInsert(dao, new BibleVersion("zh_ncv", "Chinese New Version", false));
        safeInsert(dao, new BibleVersion("eo_esperanto", "Esperanto", false));
        safeInsert(dao, new BibleVersion("fi_finnish", "Finnish (1938)", false));
        safeInsert(dao, new BibleVersion("fi_pr", "Finnish (Pyhä Raamattu)", false));
        safeInsert(dao, new BibleVersion("fr_apee", "French (Bible de l'Épée)", false));
        safeInsert(dao, new BibleVersion("de_schlachter", "German (Schlachter)", false));
        safeInsert(dao, new BibleVersion("el_greek", "Greek (Modern)", false));
        safeInsert(dao, new BibleVersion("ko_ko", "Korean", false));
        safeInsert(dao, new BibleVersion("pt_aa", "Portuguese (Almeida)", false));
        safeInsert(dao, new BibleVersion("pt_acf", "Portuguese (Corrigida Fiel)", false));
        safeInsert(dao, new BibleVersion("pt_nvi", "Portuguese (NVI)", false));
        safeInsert(dao, new BibleVersion("ro_cornilescu", "Romanian (Cornilescu)", false));
        safeInsert(dao, new BibleVersion("ru_synodal", "Russian (Synodal)", false));
        safeInsert(dao, new BibleVersion("es_rvr", "Spanish (Reina Valera)", false));
        safeInsert(dao, new BibleVersion("vi_vietnamese", "Vietnamese", false));
        safeInsert(dao, new BibleVersion("tel", "Telugu", false));

        // Keep existing ones if they were there
        safeInsert(dao, new BibleVersion("web", "World English Bible", false));
        safeInsert(dao, new BibleVersion("asv", "American Standard (1901)", false));
        safeInsert(dao, new BibleVersion("cherokee", "Cherokee New Testament", false));
    }

    private void safeInsert(BibleDao dao, BibleVersion version) {
        if (dao.getVersion(version.getId()) == null) {
            dao.insertVersion(version);
        }
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

                // Long press to download supported versions
                holder.itemView.setOnLongClickListener(v -> {
                    if (BibleDownloader.isVersionSupported(version.getId())) {
                        showDownloadOption(v, version);
                    } else {
                        Toast.makeText(getContext(), "Offline download not available for this version yet",
                                Toast.LENGTH_SHORT)
                                .show();
                    }
                    return true;
                });
            }

            holder.itemView.setOnClickListener(v -> {
                // Smart Selection for Offline-Only Versions (NIV/NLT/Telugu)
                if (!version.isDownloaded() && (version.getId().equals("niv") || version.getId().equals("nlt")
                        || version.getId().equals("tel"))) {
                    showSmartDownloadDialog(version);
                } else {
                    if (listener != null) {
                        listener.onVersionSelected(version);
                        dismiss();
                    }
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
            performDownload(version, false);
            return true;
        });
        popup.show();
    }

    private void showSmartDownloadDialog(BibleVersion version) {
        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle("Download Required")
                .setMessage(
                        "Online fetch is unavailable for " + version.getName() + ". Download now for offline access?")
                .setPositiveButton("Download", (dialog, which) -> {
                    performDownload(version, true);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performDownload(BibleVersion version, boolean autoSelect) {
        Toast.makeText(getContext(), "Downloading " + version.getName() + "...", Toast.LENGTH_SHORT).show();

        BibleDownloader.downloadVersion(getContext(), version.getId(), new BibleDownloader.DownloadCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(getContext(), "Download Complete!", Toast.LENGTH_SHORT).show();
                loadVersions(); // Refresh list to update icons

                if (autoSelect) {
                    // Auto-select the version
                    if (listener != null) {
                        listener.onVersionSelected(version);
                        dismiss();
                    }
                }
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(getContext(), "Download Failed: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }
}

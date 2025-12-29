package com.simplenotes;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface BibleDao {
    // Verse Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertVerse(Verse verse);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertVerses(List<Verse> verses);

    @Query("SELECT * FROM verses WHERE translationId = :translationId AND book = :book AND chapter = :chapter AND verse = :verse LIMIT 1")
    Verse getVerse(String translationId, String book, int chapter, int verse);

    @Query("SELECT * FROM verses WHERE translationId = :translationId AND book = :book AND chapter = :chapter AND verse BETWEEN :startVerse AND :endVerse")
    List<Verse> getVersesInRange(String translationId, String book, int chapter, int startVerse, int endVerse);

    // Version Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertVersion(BibleVersion version);

    @Query("SELECT * FROM bible_versions WHERE id = :id LIMIT 1")
    BibleVersion getVersion(String id);

    @Query("SELECT * FROM bible_versions")
    List<BibleVersion> getAllVersions();

    @Query("UPDATE bible_versions SET isDownloaded = 1 WHERE id = :id")
    void markVersionDownloaded(String id);
}

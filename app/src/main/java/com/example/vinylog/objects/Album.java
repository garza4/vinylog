package com.example.vinylog.objects;


import androidx.annotation.NonNull;
import androidx.room3.ColumnInfo;
import androidx.room3.Entity;
import androidx.room3.Ignore;
import androidx.room3.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "albums")
public class Album implements Serializable {
    @PrimaryKey(autoGenerate = true)
    long id;

    String albumName;

    @ColumnInfo(name = "artist")
    String artistName;

    @ColumnInfo(name = "songs")
    String songsOnRecord;

    @ColumnInfo(name = "year")
    String yearPublished;

    @ColumnInfo(name = "genre")
    String genre;

    @ColumnInfo(name = "media_type")
    String mediaType;

    @ColumnInfo(name = "barcode")
    String barcode;

    @NonNull
    @ColumnInfo(name = "cover_url")
    String coverUrl;

    @NonNull
    @ColumnInfo(name = "record_label")
    String recordLabel;

    @NonNull
    @ColumnInfo(name = "runtime")
    String runtime;

    @NonNull
    @ColumnInfo(name = "cast_names")
    String castNames;

    @NonNull
    @ColumnInfo(name = "page_count")
    String pageCount;

    @NonNull
    @ColumnInfo(name = "format_name")
    String formatName;

    @NonNull
    @ColumnInfo(name = "content_rating")
    String contentRating;

    @NonNull
    @ColumnInfo(name = "player_count")
    String playerCount;

    public Album(String albumName, String artistName, String songsOnRecord, String yearPublished, String genre, String mediaType, String barcode, String coverUrl, String recordLabel, String runtime, String castNames, String pageCount, String formatName, String contentRating, String playerCount) {
        this.albumName = albumName == null ? "" : albumName;
        this.artistName = artistName == null ? "" : artistName;
        this.songsOnRecord = songsOnRecord == null ? "" : songsOnRecord;
        this.yearPublished = yearPublished == null ? "" : yearPublished;
        this.genre = genre == null ? "" : genre;
        this.mediaType = mediaType == null ? "" : mediaType;
        this.barcode = barcode == null ? "" : barcode;
        this.coverUrl = coverUrl == null ? "" : coverUrl;
        this.recordLabel = recordLabel == null ? "" : recordLabel;
        this.runtime = runtime == null ? "" : runtime;
        this.castNames = castNames == null ? "" : castNames;
        this.pageCount = pageCount == null ? "" : pageCount;
        this.formatName = formatName == null ? "" : formatName;
        this.contentRating = contentRating == null ? "" : contentRating;
        this.playerCount = playerCount == null ? "" : playerCount;
    }

    @Ignore
    public Album(String albumName, String artistName, String songsOnRecord, String yearPublished, String genre, String mediaType, String barcode, String coverUrl, String recordLabel) {
        this(albumName, artistName, songsOnRecord, yearPublished, genre, mediaType, barcode, coverUrl, recordLabel, "", "", "", "", "", "");
    }

    @Ignore
    public Album(String albumName, String artistName, String songsOnRecord, String yearPublished, String genre, String mediaType, String barcode) {
        this(albumName, artistName, songsOnRecord, yearPublished, genre, mediaType, barcode, "", "");
    }

    @Ignore
    public Album(String albumName, String artistName, String songsOnRecord, String yearPublished, String genre, String mediaType) {
        this(albumName, artistName, songsOnRecord, yearPublished, genre, mediaType, "");
    }

    @Ignore
    public Album() {
        this("", "", "", "", "", "", "");
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public String getYearPublished() {
        return yearPublished;
    }

    public void setYearPublished(String yearPublished) {
        this.yearPublished = yearPublished;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getAlbumName() {
        return albumName;
    }

    public void setAlbumName(String albumName) {
        this.albumName = albumName;
    }

    public String getArtistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }

    public String getSongsOnRecord() {
        return songsOnRecord;
    }

    public void setSongsOnRecord(String songsOnRecord) {
        this.songsOnRecord = songsOnRecord;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode == null ? "" : barcode;
    }

    @NonNull
    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl == null ? "" : coverUrl;
    }

    @NonNull
    public String getRecordLabel() {
        return recordLabel;
    }

    public void setRecordLabel(String recordLabel) {
        this.recordLabel = recordLabel == null ? "" : recordLabel;
    }

    @NonNull
    public String getRuntime() {
        return runtime;
    }

    public void setRuntime(String runtime) {
        this.runtime = runtime == null ? "" : runtime;
    }

    @NonNull
    public String getCastNames() {
        return castNames;
    }

    public void setCastNames(String castNames) {
        this.castNames = castNames == null ? "" : castNames;
    }

    @NonNull
    public String getPageCount() {
        return pageCount;
    }

    public void setPageCount(String pageCount) {
        this.pageCount = pageCount == null ? "" : pageCount;
    }

    @NonNull
    public String getFormatName() {
        return formatName;
    }

    public void setFormatName(String formatName) {
        this.formatName = formatName == null ? "" : formatName;
    }

    @NonNull
    public String getContentRating() {
        return contentRating;
    }

    public void setContentRating(String contentRating) {
        this.contentRating = contentRating == null ? "" : contentRating;
    }

    @NonNull
    public String getPlayerCount() {
        return playerCount;
    }

    public void setPlayerCount(String playerCount) {
        this.playerCount = playerCount == null ? "" : playerCount;
    }

    @Override
    public String toString() {
        String name = albumName == null ? "" : albumName;
        String artist = artistName == null ? "" : artistName;
        String year = yearPublished == null || yearPublished.isEmpty() ? "" : " (" + yearPublished + ")";
        if (artist.isEmpty()) {
            return name + year;
        }
        return name + year + " — " + artist;
    }
}

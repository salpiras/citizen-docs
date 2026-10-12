package com.salpiras.citizendocs.core.drive

import kotlinx.serialization.Serializable

/**
 * The wire shapes, kept internal so Drive's JSON never becomes anyone else's problem.
 *
 * Only the fields this app sends or reads. Drive's file resource has dozens more; asking
 * for them with `fields` would cost bytes on every response for data nothing uses, and
 * modelling them here would invite someone to start depending on them.
 */
@Serializable
internal data class FileResource(
    val id: String,
    val name: String = "",
    val appProperties: Map<String, String> = emptyMap(),
)

internal fun FileResource.asDriveFile() = DriveFile(id = id, name = name, appProperties = appProperties)

@Serializable
internal data class FileList(
    val files: List<FileResource> = emptyList(),
    /** Absent on the last page. Its absence is the only way to know the walk is finished. */
    val nextPageToken: String? = null,
)

/**
 * The metadata part of an upload, and the body of a folder creation.
 *
 * Nulls are dropped rather than sent: Drive treats an explicit `"parents": null` on an
 * update as an error, and omitting a field means "leave it alone", which is what an update
 * of name and bytes actually wants.
 */
@Serializable
internal data class FileMetadata(
    val name: String,
    val mimeType: String? = null,
    val parents: List<String>? = null,
    val appProperties: Map<String, String>? = null,
)

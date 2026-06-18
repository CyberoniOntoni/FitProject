import Foundation
import FirebaseStorage

final class FirebaseStorageService {
    static let shared = FirebaseStorageService()
    private let storage = Storage.storage()

    private init() {}

    func uploadProgressPhoto(
        userId: String,
        sessionId: String,
        poseType: String,
        data: Data,
        contentType: String,
        fileExtension: String
    ) async throws -> String {
        let objectPath = "progressPictures/\(userId)/session_\(sessionId)_\(poseType).\(fileExtension)"
        let ref = storage.reference().child(objectPath)
        let metadata = StorageMetadata()
        metadata.contentType = contentType

        _ = try await ref.putDataAsync(data, metadata: metadata)
        let url = try await ref.downloadURL()
        return url.absoluteString
    }
}
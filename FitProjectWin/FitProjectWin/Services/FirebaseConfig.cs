namespace FitProjectWin.Services;

public static class FirebaseConfig
{
    public const string ApiKey = "AIzaSyDcR92CtVhDqtxeEGRu_8JGuGw2UicjQQ0";
    public const string ProjectId = "workouts-67e5d";
    public const string AuthBaseUrl = "https://identitytoolkit.googleapis.com/v1";
    public const string FirestoreBaseUrl = $"https://firestore.googleapis.com/v1/projects/{ProjectId}/databases/(default)/documents";
    public const string StorageBucket = "workouts-67e5d.appspot.com";
    public const string StorageBaseUrl = $"https://firebasestorage.googleapis.com/v0/b/{StorageBucket}/o";
}
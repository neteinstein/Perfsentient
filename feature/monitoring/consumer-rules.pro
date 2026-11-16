# Workers are instantiated by Koin's WorkerFactory using the class name stored by WorkManager.
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters, ...);
}

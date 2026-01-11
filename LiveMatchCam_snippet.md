/*navController.addOnDestinationChangedListener { controller, destination, arguments ->
    try {
        val rootId = navController.graph.id
        val backStackEntries = mutableListOf<String>()

        var currentEntry: NavBackStackEntry? = navController.currentBackStackEntry
        while (currentEntry != null) {
            backStackEntries.add(currentEntry.destination.label.toString())
            if (currentEntry.destination.id == rootId) break
            currentEntry = try {
                navController.getBackStackEntry(currentEntry.destination.id - 1)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
        Log.d("LM_NAV_DEBUG", "Back stack: ${backStackEntries.joinToString(" -> ")}")
    } catch (e: Exception) {
        Log.e("LM_NAV_DEBUG", "Error reading back stack", e)
    }
}*/

/*
    val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    val ids = cameraManager.cameraIdList
    for (cameraId in ids) {
        val cameraFacing = cameraManager.getCameraCharacteristics(cameraId).get(CameraCharacteristics.LENS_FACING)
        if (cameraFacing != null && cameraFacing == CameraMetadata.LENS_FACING_BACK) {
        var characteristics = cameraManager.getCameraCharacteristics(cameraId)
        val maxZoom = characteristics.secureGet(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1f
    }
}
*/
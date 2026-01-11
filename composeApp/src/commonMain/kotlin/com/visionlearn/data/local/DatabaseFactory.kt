package com.visionlearn.data.local

import app.cash.sqldelight.db.SqlDriver

/**
 * Platform-specific database driver factory
 */
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

/**
 * Creates the VisionLearnDatabase instance
 */
fun createDatabase(driverFactory: DatabaseDriverFactory): VisionLearnDatabase {
    val driver = driverFactory.createDriver()
    return VisionLearnDatabase(driver)
}

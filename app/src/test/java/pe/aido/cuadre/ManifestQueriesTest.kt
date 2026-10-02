package pe.aido.cuadre

import org.junit.Assert.assertTrue
import org.junit.Test
import pe.aido.cuadre.domain.Wallet
import java.io.File

/** Every wallet package must be declared in <queries>, or the battery check can't see the app. */
class ManifestQueriesTest {
    @Test fun everyWalletPackageIsQueryable() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        Wallet.entries.flatMap { it.packages }.forEach { pkg ->
            assertTrue("missing <package android:name=\"$pkg\" /> in <queries>", manifest.contains("\"$pkg\""))
        }
    }
}

package br.com.nexj.nexsong.player

import android.content.Context
import android.content.SharedPreferences
import android.media.audiofx.Equalizer

/**
 * Equalizador nativo do Android ligado à sessão de áudio do player.
 * As configurações ficam salvas no aparelho. Todas as chamadas são protegidas,
 * porque alguns aparelhos não oferecem equalizador.
 */
object EqualizerManager {

    private var equalizer: Equalizer? = null
    private var prefs: SharedPreferences? = null

    fun attach(context: Context, audioSessionId: Int) {
        release()
        val p = context.getSharedPreferences("nexsong_equalizer", Context.MODE_PRIVATE)
        prefs = p
        try {
            val eq = Equalizer(0, audioSessionId)
            val range = eq.bandLevelRange
            val min = range[0].toInt()
            val max = range[1].toInt()
            val bands = eq.numberOfBands.toInt()
            for (band in 0 until bands) {
                val saved = p.getInt("band_$band", Int.MIN_VALUE)
                if (saved != Int.MIN_VALUE) {
                    eq.setBandLevel(band.toShort(), saved.coerceIn(min, max).toShort())
                }
            }
            eq.setEnabled(p.getBoolean("enabled", false))
            equalizer = eq
        } catch (e: Exception) {
            equalizer = null
        }
    }

    fun release() {
        try {
            equalizer?.release()
        } catch (e: Exception) {
            // sem ação
        }
        equalizer = null
    }

    fun isAvailable(): Boolean = equalizer != null

    fun isEnabled(): Boolean = try {
        equalizer?.enabled ?: false
    } catch (e: Exception) {
        false
    }

    fun setEnabled(value: Boolean) {
        try {
            equalizer?.setEnabled(value)
            prefs?.edit()?.putBoolean("enabled", value)?.apply()
        } catch (e: Exception) {
            // sem ação
        }
    }

    fun bandCount(): Int = try {
        equalizer?.numberOfBands?.toInt() ?: 0
    } catch (e: Exception) {
        0
    }

    /** Limites em milibéis (100 = 1 dB). */
    fun minLevel(): Int = try {
        equalizer?.bandLevelRange?.get(0)?.toInt() ?: -1500
    } catch (e: Exception) {
        -1500
    }

    fun maxLevel(): Int = try {
        equalizer?.bandLevelRange?.get(1)?.toInt() ?: 1500
    } catch (e: Exception) {
        1500
    }

    /** Frequência central da banda, em Hz. */
    fun centerFreqHz(band: Int): Int = try {
        (equalizer?.getCenterFreq(band.toShort()) ?: 0) / 1000
    } catch (e: Exception) {
        0
    }

    fun levels(): List<Int> {
        val eq = equalizer ?: return emptyList()
        return try {
            List(eq.numberOfBands.toInt()) { band -> eq.getBandLevel(band.toShort()).toInt() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun setBandLevel(band: Int, level: Int) {
        try {
            equalizer?.setBandLevel(band.toShort(), level.toShort())
            prefs?.edit()?.putInt("band_$band", level)?.apply()
        } catch (e: Exception) {
            // sem ação
        }
    }

    fun presets(): List<String> {
        val eq = equalizer ?: return emptyList()
        return try {
            List(eq.numberOfPresets.toInt()) { index -> eq.getPresetName(index.toShort()) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun usePreset(index: Int) {
        val eq = equalizer ?: return
        try {
            eq.usePreset(index.toShort())
            val editor = prefs?.edit()
            for (band in 0 until eq.numberOfBands.toInt()) {
                editor?.putInt("band_$band", eq.getBandLevel(band.toShort()).toInt())
            }
            editor?.apply()
        } catch (e: Exception) {
            // sem ação
        }
    }
}

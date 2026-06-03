package com.shieldcheck.app.data.repository

import com.shieldcheck.app.data.model.StolenObject
import io.github.supabase.supabase_kt.SupabaseClient
import io.github.supabase.realtime_kt.PostgresAction
import io.github.supabase.realtime_kt.RealtimeChannel
import io.github.supabase.realtime_kt.listening
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.serialization.json.jsonObject

class StolenObjectRepository(private val supabaseClient: SupabaseClient) {

    private val stolenObjectUpdates = MutableSharedFlow<StolenObject>()

    /**
     * Écoute les mises à jour en temps réel de la table objets_voles
     */
    fun observeStolenObjects(): Flow<StolenObject> = stolenObjectUpdates

    /**
     * Initialise l'écoute Realtime sur la table objets_voles
     */
    suspend fun startListening() {
        try {
            val channel = supabaseClient.realtime.createChannel("public:objets_voles")
            
            channel.onPostgresChange(
                event = PostgresAction.ALL,
                schema = "public",
                table = "objets_voles"
            ) { message ->
                val payload = message.payload.new?.jsonObject
                if (payload != null) {
                    try {
                        val stolenObject = StolenObject(
                            id = payload["id"].toString().trim('"'),
                            imei = payload["imei"].toString().trim('"'),
                            status = payload["status"].toString().trim('"'),
                            owner_name = payload["owner_name"].toString().trim('"'),
                            owner_email = payload["owner_email"].toString().trim('"'),
                            phoneModel = payload["phone_model"].toString().trim('"'),
                            createdAt = payload["created_at"].toString().trim('"'),
                            updatedAt = payload["updated_at"]?.toString()?.trim('"'),
                            description = payload["description"]?.toString()?.trim('"')
                        )
                        stolenObjectUpdates.emit(stolenObject)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            channel.subscribe { status ->
                println("Channel status: $status")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Récupère tous les objets volés avec le statut 'vole'
     */
    suspend fun getStolenObjectsWithStatus(status: String = "vole"): List<StolenObject> {
        return try {
            supabaseClient.postgrest
                .from("objets_voles")
                .select()
                .decodeList<StolenObject>()
                .filter { it.status == status }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Cherche un objet volé par IMEI
     */
    suspend fun findStolenObjectByIMEI(imei: String): StolenObject? {
        return try {
            supabaseClient.postgrest
                .from("objets_voles")
                .select()
                .eq("imei", imei)
                .limit(1)
                .decodeList<StolenObject>()
                .firstOrNull()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
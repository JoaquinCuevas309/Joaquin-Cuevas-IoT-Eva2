package cl.inacap.securelock.data.demo

import android.content.Context
import cl.inacap.securelock.data.model.Usuario
import org.json.JSONArray
import org.json.JSONObject

data class StoredUser(
    val uid: String,
    val nombre: String,
    val email: String,
    val lockers: List<String>,
    val salt: String,
    val hash: String,
) {
    fun toUsuario() = Usuario(uid = uid, nombre = nombre, email = email, lockersAsignados = lockers)
}

interface UserStore {
    fun loadUsers(): List<StoredUser>?
    fun saveUsers(users: List<StoredUser>)
    fun loadSession(): String?
    fun saveSession(uid: String?)
}

class InMemoryUserStore : UserStore {
    private var users: List<StoredUser>? = null
    private var session: String? = null
    override fun loadUsers() = users
    override fun saveUsers(users: List<StoredUser>) { this.users = users }
    override fun loadSession() = session
    override fun saveSession(uid: String?) { session = uid }
}

// persistencia local del modo demo
class PrefsUserStore(context: Context) : UserStore {
    private val prefs = context.applicationContext.getSharedPreferences("securelock_demo", Context.MODE_PRIVATE)

    override fun loadUsers(): List<StoredUser>? {
        val raw = prefs.getString(KEY_USERS, null) ?: return null
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                val lockers = o.getJSONArray("lockers")
                StoredUser(
                    uid = o.getString("uid"),
                    nombre = o.getString("nombre"),
                    email = o.getString("email"),
                    lockers = (0 until lockers.length()).map { lockers.getString(it) },
                    salt = o.getString("salt"),
                    hash = o.getString("hash"),
                )
            }
        }.getOrNull()
    }

    override fun saveUsers(users: List<StoredUser>) {
        val array = JSONArray()
        users.forEach { u ->
            array.put(
                JSONObject()
                    .put("uid", u.uid)
                    .put("nombre", u.nombre)
                    .put("email", u.email)
                    .put("lockers", JSONArray(u.lockers))
                    .put("salt", u.salt)
                    .put("hash", u.hash)
            )
        }
        prefs.edit().putString(KEY_USERS, array.toString()).apply()
    }

    override fun loadSession(): String? = prefs.getString(KEY_SESSION, null)

    override fun saveSession(uid: String?) {
        prefs.edit().apply { if (uid == null) remove(KEY_SESSION) else putString(KEY_SESSION, uid) }.apply()
    }

    private companion object {
        const val KEY_USERS = "users"
        const val KEY_SESSION = "session"
    }
}

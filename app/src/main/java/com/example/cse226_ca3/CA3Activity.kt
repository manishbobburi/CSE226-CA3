package com.example.cse226_ca3

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.lifecycle.viewmodel.compose.viewModel

class CA3Activity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {

        }
    }
}

@Entity
data class DairyEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val content: String,
)


@Dao
interface DairyDao {
    @Insert
    suspend fun insertEntry(entry: DairyEntry)

    @Query("SELECT * FROM DairyEntry")
    suspend fun getAllEntries(): List<DairyEntry>

    @Query("SELECT COUNT(*) FROM DairyEntry")
    suspend fun countEntries(): Int
}

@Database([DairyEntry::class], version=1)
abstract class AppDatabase: RoomDatabase() {
    companion object {
        val DB_NAME = "Dairy_DB"
    }

    abstract fun dairyDao(): DairyDao
}

class MainApplication: Application() {
    companion object {
        lateinit var appDatabase: AppDatabase
    }

    override fun onCreate() {
        super.onCreate()

        appDatabase = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            AppDatabase.DB_NAME
        ).build()
    }
}


class DairyEntryViewModel: ViewModel() {
    private val dairyDao = MainApplication.appDatabase.dairyDao()

    var dairyEntries by mutableStateOf<List<DairyEntry>> (emptyList())
    var count by mutableStateOf<Int> (0)

    fun insertEntry(title: String, content: String) {
        viewModelScope.launch {
            dairyDao.insertEntry(DairyEntry(title = title, content = content))
            getAllEntries()
            countEntries()
        }
    }

    fun getAllEntries() {
        viewModelScope.launch {
            dairyEntries = dairyDao.getAllEntries()
        }
    }

    fun countEntries() {
        viewModelScope.launch {
            count = dairyDao.countEntries()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DairyEntryScreen(dairyEntryViewModel: DairyEntryViewModel = viewModel()) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var isLimitExceeded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Enter title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Enter content") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        if (dairyEntryViewModel.count >= 3) {
                            isLimitExceeded = true
                        } else {
                            dairyEntryViewModel.insertEntry(title, content)
                            title = ""
                            content = ""
                            isLimitExceeded = false
                        }
                    },
                    enabled = title.isNotBlank() && content.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add Entry")
                }

                if (isLimitExceeded) {
                    Text(
                        text = "3 entries limit exceeded",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text(
                text = "Your Entries",
                style = MaterialTheme.typography.titleMedium
            )
        }

        items(dairyEntryViewModel.dairyEntries) { dairyEntry ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = dairyEntry.title,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = dairyEntry.content,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

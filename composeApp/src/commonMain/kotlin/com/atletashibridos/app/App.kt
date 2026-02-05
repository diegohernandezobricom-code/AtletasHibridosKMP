package com.atletashibridos.app

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.UUID

// PALETA DE COLORES "ATLETAS HÍBRIDOS"
val AzulNoche = Color(0xFF0D1B2A)
val GrisFondo = Color(0xFFE0E1DD)
val GrisPlata = Color(0xFF778DA9)
val BlancoPuro = Color(0xFFFFFFFF)

data class Evento(
    val id: String = UUID.randomUUID().toString(),
    val nombre: String,
    val deporte: String,
    val jugadores: MutableList<Jugador> = mutableListOf(),
    var costoTotal: String = ""
)

data class Jugador(
    val id: Long = System.currentTimeMillis(),
    val nombre: String,
    val celular: String,
    var pagado: Boolean = false
)

@Composable
fun App() {
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("AH_Eventos_Prefs", Context.MODE_PRIVATE) }
    val listaEventos = remember { mutableStateListOf<Evento>() }
    var eventoSeleccionado by remember { mutableStateOf<Evento?>(null) }
    var mostrarDialogoNuevo by remember { mutableStateOf(false) }

    val guardarTodo = {
        val editor = sharedPreferences.edit()
        val data = listaEventos.joinToString("###") { evento ->
            val jugs = evento.jugadores.joinToString(";") { "${it.id},${it.nombre},${it.celular},${it.pagado}" }
            "${evento.id}|${evento.nombre}|${evento.deporte}|${evento.costoTotal}|$jugs"
        }
        editor.putString("data_completa", data)
        editor.apply()
    }

    LaunchedEffect(Unit) {
        val raw = sharedPreferences.getString("data_completa", "") ?: ""
        if (raw.isNotEmpty()) {
            raw.split("###").forEach { evStr ->
                val partes = evStr.split("|")
                if (partes.size >= 4) {
                    val evento = Evento(partes[0], partes[1], partes[2], mutableListOf(), partes[3])
                    if (partes.size == 5 && partes[4].isNotEmpty()) {
                        partes[4].split(";").forEach { jStr ->
                            val d = jStr.split(",")
                            if (d.size == 4) evento.jugadores.add(Jugador(d[0].toLong(), d[1], d[2], d[3].toBoolean()))
                        }
                    }
                    listaEventos.add(evento)
                }
            }
        }
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = GrisFondo) {
            if (eventoSeleccionado == null) {
                Scaffold(
                    containerColor = GrisFondo,
                    floatingActionButton = {
                        FloatingActionButton(onClick = { mostrarDialogoNuevo = true }, containerColor = AzulNoche) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = BlancoPuro)
                        }
                    }
                ) { padding ->
                    Column(modifier = Modifier.padding(padding).padding(20.dp)) {
                        Text("ATLETAS HÍBRIDOS", color = AzulNoche, fontSize = 26.sp, fontWeight = FontWeight.Black)
                        Text("Mis Cuentas", color = GrisPlata, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(24.dp))
                        LazyColumn {
                            items(listaEventos) { evento ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { eventoSeleccionado = evento },
                                    colors = CardDefaults.cardColors(containerColor = BlancoPuro),
                                    elevation = CardDefaults.cardElevation(2.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(evento.nombre, color = AzulNoche, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                                            Text(evento.deporte.uppercase(), color = GrisPlata, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                        IconButton(onClick = { listaEventos.remove(evento); guardarTodo() }) {
                                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFB00020))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                PantallaDetalle(eventoSeleccionado!!, onBack = { eventoSeleccionado = null }, onUpdate = { guardarTodo() })
            }
        }

        if (mostrarDialogoNuevo) {
            var nNombre by remember { mutableStateOf("") }
            var nDeporte by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { mostrarDialogoNuevo = false },
                containerColor = BlancoPuro,
                title = { Text("Nueva Cuenta", color = AzulNoche, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        OutlinedTextField(value = nNombre, onValueChange = { nNombre = it }, label = { Text("Nombre") })
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = nDeporte, onValueChange = { nDeporte = it }, label = { Text("Deporte") })
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (nNombre.isNotBlank()) {
                            listaEventos.add(Evento(nombre = nNombre, deporte = nDeporte))
                            guardarTodo()
                            mostrarDialogoNuevo = false
                        }
                    }, colors = ButtonDefaults.buttonColors(containerColor = AzulNoche)) { Text("Crear", color = BlancoPuro) }
                }
            )
        }
    }
}

@Composable
fun PantallaDetalle(evento: Evento, onBack: () -> Unit, onUpdate: () -> Unit) {
    var nombreJ by remember { mutableStateOf("") }
    var celularJ by remember { mutableStateOf("") }
    var costoCancha by remember { mutableStateOf(evento.costoTotal) }
    val jugadores = remember { mutableStateListOf<Jugador>().apply { addAll(evento.jugadores) } }

    val esValido = nombreJ.isNotBlank() && celularJ.length == 9

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).statusBarsPadding()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = AzulNoche) }
            Text(evento.nombre, color = AzulNoche, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = BlancoPuro)) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(value = nombreJ, onValueChange = { nombreJ = it }, label = { Text("Nombre Jugador") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = celularJ, onValueChange = { if (it.length <= 9) celularJ = it }, label = { Text("Celular (9 dígitos)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                Button(onClick = {
                    if (esValido) {
                        val nuevo = Jugador(nombre = nombreJ, celular = celularJ)
                        jugadores.add(nuevo)
                        evento.jugadores.add(nuevo)
                        nombreJ = ""; celularJ = ""; onUpdate()
                    }
                }, enabled = esValido, modifier = Modifier.fillMaxWidth().padding(top = 12.dp), colors = ButtonDefaults.buttonColors(containerColor = AzulNoche, disabledContainerColor = GrisPlata)) {
                    Text("AÑADIR A LA LISTA", fontWeight = FontWeight.Bold, color = BlancoPuro)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(jugadores) { j ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = BlancoPuro.copy(alpha = 0.6f)), shape = RoundedCornerShape(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                        Checkbox(checked = j.pagado, onCheckedChange = {
                            val idx = jugadores.indexOf(j)
                            jugadores[idx] = j.copy(pagado = it)
                            evento.jugadores.find { it.id == j.id }?.pagado = it
                            onUpdate()
                        }, colors = CheckboxDefaults.colors(checkedColor = AzulNoche))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(j.nombre, fontWeight = FontWeight.Bold, color = AzulNoche)
                            Text(j.celular, fontSize = 12.sp, color = GrisPlata)
                        }
                        IconButton(onClick = { jugadores.remove(j); evento.jugadores.removeIf { it.id == j.id }; onUpdate() }) {
                            Icon(Icons.Default.Delete, null, tint = Color(0xFFB00020), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        // CUADRO AZUL NOCHE CON EL TOTAL DE PERSONAS
        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            colors = CardDefaults.cardColors(containerColor = AzulNoche),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                OutlinedTextField(
                    value = costoCancha,
                    onValueChange = { costoCancha = it; evento.costoTotal = it; onUpdate() },
                    label = { Text("COSTO TOTAL CANCHA", color = GrisPlata, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = BlancoPuro, unfocusedTextColor = BlancoPuro, focusedBorderColor = GrisPlata, unfocusedBorderColor = GrisPlata.copy(alpha = 0.5f)),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                val total = costoCancha.toDoubleOrNull() ?: 0.0
                val n = jugadores.size
                val divisor = if(n == 0) 1 else n
                val toca = total / divisor

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Column {
                        Text("TOTAL PARTICIPANTES", color = GrisPlata, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("$n personas", color = BlancoPuro, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("CADA UNO PAGA", color = GrisPlata, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("S/ ${String.format("%.2f", toca)}", color = BlancoPuro, fontSize = 28.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}
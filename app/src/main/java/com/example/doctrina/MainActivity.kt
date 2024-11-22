package com.example.doctrina

import android.Manifest
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var editTextUbicacion: TextInputEditText
    private val LOCATION_PERMISSION_REQUEST_CODE = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar el cliente de ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Configurar Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Inicializar vistas
        val spinnerTipoProblema = findViewById<AutoCompleteTextView>(R.id.spinnerTipoProblema)
        val editTextDescripcion = findViewById<TextInputEditText>(R.id.editTextDescripcion)
        editTextUbicacion = findViewById(R.id.editTextUbicacion)
        val buttonEnviar = findViewById<MaterialButton>(R.id.buttonEnviar)

        val problemas = arrayOf(
            "Contaminación del aire",
            "Acumulación de basura",
            "Contaminación del agua",
            "Otro"
        )

        val adapter = ArrayAdapter(
            this,
            R.layout.dropdown_menu_item,
            problemas
        )
        spinnerTipoProblema.setAdapter(adapter)

        // Configurar el click en el botón de ubicación
        val buttonUbicacion = findViewById<MaterialButton>(R.id.buttonUbicacion)
        buttonUbicacion.setOnClickListener {
            requestLocation()
        }

        buttonEnviar.setOnClickListener {
            val tipoProblema = spinnerTipoProblema.text.toString()
            val descripcion = editTextDescripcion.text.toString()
            val ubicacion = editTextUbicacion.text.toString()

            if (tipoProblema.isEmpty() || descripcion.isEmpty() || ubicacion.isEmpty()) {
                showErrorDialog("Por favor complete todos los campos")
                return@setOnClickListener
            }

            showSuccessDialog()
        }
    }

    private fun showSuccessDialog() {
        MaterialAlertDialogBuilder(this)
            .setIcon(R.drawable.ic_success)
            .setTitle("¡Éxito!")
            .setMessage("Reporte enviado exitosamente")
            .setPositiveButton("Aceptar") { dialog, _ ->
                dialog.dismiss()
                // Limpiar campos
                findViewById<AutoCompleteTextView>(R.id.spinnerTipoProblema).text.clear()
                findViewById<TextInputEditText>(R.id.editTextDescripcion).text?.clear()
                findViewById<TextInputEditText>(R.id.editTextUbicacion).text?.clear()
            }
            .show()
    }

    private fun showErrorDialog(message: String) {
        MaterialAlertDialogBuilder(this)
            .setIcon(R.drawable.ic_warning)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("Aceptar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun requestLocation() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                getLocation()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                showLocationPermissionRationale()
            }
            else -> {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    private fun showLocationPermissionRationale() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Permiso de ubicación necesario")
            .setMessage("Necesitamos acceder a tu ubicación para poder registrar el lugar exacto del problema ambiental. ¿Deseas habilitarlo?")
            .setPositiveButton("Sí") { _, _ ->
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_PERMISSION_REQUEST_CODE
                )
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun getLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val cancellationToken = com.google.android.gms.tasks.CancellationTokenSource().token

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationToken)
            .addOnSuccessListener { location ->
                if (location != null) {
                    val geocoder = Geocoder(this, Locale.getDefault())
                    try {
                        val addresses: List<Address>? = geocoder.getFromLocation(
                            location.latitude,
                            location.longitude,
                            1
                        )
                        if (!addresses.isNullOrEmpty()) {
                            val address = addresses[0]
                            val addressText = address.getAddressLine(0)
                            editTextUbicacion.setText(addressText)
                        } else {
                            val coordinatesText = "${location.latitude}, ${location.longitude}"
                            editTextUbicacion.setText(coordinatesText)
                        }
                    } catch (e: Exception) {
                        editTextUbicacion.setText("${location.latitude}, ${location.longitude}")
                    }
                }
            }
            .addOnFailureListener {
                showErrorDialog("Error al obtener la ubicación: ${it.message}")
            }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
            grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            getLocation()
        }
    }
}

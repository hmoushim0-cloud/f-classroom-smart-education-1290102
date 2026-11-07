package sa.factory.fclassroomsmartedu.data.repository

import sa.factory.fclassroomsmartedu.domain.repository.FClassroomRepository
import sa.factory.fclassroomsmartedu.hardware.HardwareSensorDriver
import sa.factory.fclassroomsmartedu.model.FClassroomItem
import sa.factory.fclassroomsmartedu.model.FClassroomOrder
import sa.factory.fclassroomsmartedu.model.FClassroomTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class FClassroomRepositoryImpl(
    private val sensorDriver: HardwareSensorDriver
) : FClassroomRepository {

    private val _catalog = MutableStateFlow<List<FClassroomItem>>(listOf(
        FClassroomItem("1", "الباقة التنفيذية المعتمدة", "تكامل مباشر مع Smart Stylus Digitizer 4096-Level + BLE Beacon", 180.0, "معتمد", "الخدمات الرئيسية"),
        FClassroomItem("2", "خدمة المعالجة الفورية", "أداء أصيل بسرعة 60 إطار في الثانية", 95.0, "فوري", "المراقبة الحية"),
        FClassroomItem("3", "باقة الصيانة والدعم السحابي", "تشفير كامل للبيانات المحلية ومزامنة آمنة", 250.0, "شامل", "التقارير الضريبية")
    ))
    override val catalogFlow: Flow<List<FClassroomItem>> = _catalog.asStateFlow()

    private val _cart = MutableStateFlow<List<FClassroomItem>>(emptyList())
    override val activeCart: StateFlow<List<FClassroomItem>> = _cart.asStateFlow()

    override val telemetryFlow: Flow<FClassroomTelemetry> = sensorDriver.observePeripheralStream()
        .map { frame ->
            FClassroomTelemetry(
                peripheralName = frame.peripheralName,
                status = frame.connectionState,
                liveReading = frame.livePayload,
                batteryPct = 96,
                signalDbm = -54
            )
        }

    override fun addToCart(item: FClassroomItem) {
        val current = _cart.value.toMutableList()
        current.add(item)
        _cart.value = current
    }

    override fun removeFromCart(item: FClassroomItem) {
        val current = _cart.value.toMutableList()
        current.remove(item)
        _cart.value = current
    }

    override fun clearCart() {
        _cart.value = emptyList()
    }

    override suspend fun checkout(paymentMethod: String): FClassroomOrder {
        val currentItems = _cart.value.toList()
        val subtotal = currentItems.sumOf { it.priceSar }
        val vat = subtotal * 0.15
        val total = subtotal + vat
        val order = FClassroomOrder(
            orderId = "ORD-${System.currentTimeMillis() % 100000}",
            items = currentItems,
            subtotalSar = subtotal,
            vatSar = vat,
            totalSar = total,
            paymentMethod = paymentMethod
        )
        clearCart()
        return order
    }

    override suspend fun refreshCatalog() {
        // Offline-first sync logic
    }
}

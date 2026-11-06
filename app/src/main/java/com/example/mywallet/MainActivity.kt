package com.example.mywallet

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mywallet.ui.theme.MyWalletTheme
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

// --- DATA CLASS & SAMPLE DATA ---

data class Expense(
    val id: String = UUID.randomUUID().toString(),
    val amount: Double,
    val currency: String, // "KHR" or "USD"
    val category: String,
    val date: LocalDate,
    val remark: String? = null
)

val sampleExpenses = listOf(
    Expense(amount = 4.50, currency = "USD", category = "Food", date = LocalDate.now(), remark = "Lunch"),
    Expense(amount = 12000.0, currency = "KHR", category = "Transport", date = LocalDate.now()),
    Expense(amount = 3.00, currency = "USD", category = "Food", date = LocalDate.now(), remark = "Coffee"),
    Expense(amount = 20.0, currency = "USD", category = "Shopping", date = LocalDate.now())
)

// --- MAIN ACTIVITY ---

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    private val TAG = "MyWallet"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        setContent {
            MyWalletTheme {
                // STATE LIVES HERE (Caller)
                val expenseList = remember { mutableStateListOf<Expense>().apply { addAll(sampleExpenses) } }
                var lastDeletedExpense by remember { mutableStateOf<Expense?>(null) }
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()
                val context = LocalContext.current

                // Work out total and category totals from the list
                val total = expenseList.sumOf { it.amount }
                val categoryTotals = expenseList.groupBy { it.category }
                    .mapValues { entry -> entry.value.sumOf { it.amount } }

                // Empty state using derivedStateOf
                val isEmpty by remember { derivedStateOf { expenseList.isEmpty() } }

                Scaffold(
                    topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
                    floatingActionButton = {
                        FloatingActionButton(onClick = {
                            // D5: Add Expense
                            val newExpense = Expense(
                                amount = (10..100).random().toDouble(),
                                currency = "USD",
                                category = "Misc",
                                date = LocalDate.now()
                            )
                            expenseList.add(newExpense)
                            Log.d(TAG, "Added expense: ${newExpense.id}")
                        }) {
                            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_expense))
                        }
                    },
                    snackbarHost = {
                        // Snackbar just shows the data. The logic is handled in showSnackbar()!
                        SnackbarHost(hostState = snackbarHostState) { data ->
                            Snackbar(snackbarData = data)
                        }
                    }
                ) { padding ->
                    // PASS PARAMETERS TO STATELESS COMPOSABLE
                    DashboardScreen(
                        modifier = Modifier.padding(padding),
                        expenses = expenseList,
                        total = total,
                        categoryTotals = categoryTotals,
                        isEmpty = isEmpty,
                        onDelete = { expense ->
                            expenseList.remove(expense)
                            lastDeletedExpense = expense
                            Log.d(TAG, "Deleted expense: ${expense.id}")

                            // Show snackbar and wait for user action (Undo)
                            scope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = context.getString(R.string.expense_deleted),
                                    actionLabel = context.getString(R.string.undo)
                                )
                                // If the user clicks Undo, put the item back
                                if (result == SnackbarResult.ActionPerformed) {
                                    lastDeletedExpense?.let {
                                        expenseList.add(it)
                                        Log.d(TAG, "Undid delete: ${it.id}")
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    // Lifecycle Logs
    override fun onStart() { super.onStart(); Log.d(TAG, "onStart") }
    override fun onResume() { super.onResume(); Log.d(TAG, "onResume") }
    override fun onPause() { super.onPause(); Log.d(TAG, "onPause") }
    override fun onStop() { super.onStop(); Log.d(TAG, "onStop") }
    override fun onDestroy() { super.onDestroy(); Log.d(TAG, "onDestroy") }
}

// --- UI COMPOSABLES ---

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    expenses: List<Expense>,
    total: Double,
    categoryTotals: Map<String, Double>,
    isEmpty: Boolean,
    onDelete: (Expense) -> Unit
) {
    Column(
        modifier = modifier
            .padding(dimensionResource(R.dimen.padding_medium))
            .fillMaxSize()
    ) {
        TotalSection(total)
        Spacer(Modifier.height(dimensionResource(R.dimen.padding_medium)))
        CategorySection(categoryTotals)
        Spacer(Modifier.height(dimensionResource(R.dimen.padding_medium)))

        if (isEmpty) {
            EmptyState()
        } else {
            ExpenseList(expenses, onDelete)
        }
    }
}

@Composable
fun TotalSection(total: Double) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(dimensionResource(R.dimen.padding_medium))) {
            Text(stringResource(R.string.this_month_total), style = MaterialTheme.typography.labelMedium)
            Text(
                text = "$$total",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun CategorySection(categoryTotals: Map<String, Double>) {
    val maxAmount = categoryTotals.values.maxOrNull() ?: 1.0
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(dimensionResource(R.dimen.padding_medium))) {
            Text(stringResource(R.string.spending_by_category), style = MaterialTheme.typography.titleMedium)
            categoryTotals.forEach { (category, amount) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = dimensionResource(R.dimen.padding_small)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(category, modifier = Modifier.width(80.dp), style = MaterialTheme.typography.bodyMedium)
                    Box(modifier = Modifier.weight(1f).height(dimensionResource(R.dimen.bar_height)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                        Box(modifier = Modifier.fillMaxWidth((amount / maxAmount).toFloat()).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
                    }
                    Spacer(Modifier.width(dimensionResource(R.dimen.padding_small)))
                    Text("$$amount", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun ExpenseList(expenses: List<Expense>, onDelete: (Expense) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(dimensionResource(R.dimen.padding_medium))) {
            Text(stringResource(R.string.recent_expenses), style = MaterialTheme.typography.titleMedium)
            LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                items(expenses) { expense ->
                    ExpenseRow(expense, onDelete)
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun ExpenseRow(expense: Expense, onDelete: (Expense) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = dimensionResource(R.dimen.padding_small)),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(expense.category, style = MaterialTheme.typography.bodyLarge)
            Text(expense.date.toString(), style = MaterialTheme.typography.bodySmall)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${expense.currency} ${expense.amount}", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.width(dimensionResource(R.dimen.padding_small)))
            IconButton(onClick = { onDelete(expense) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}

@Composable
fun EmptyState() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.padding(dimensionResource(R.dimen.padding_medium))) {
            Text(stringResource(R.string.no_expenses_yet), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ExpenseRowPreview() {
    MyWalletTheme {
        ExpenseRow(sampleExpenses.first(), onDelete = {})
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
fun DashboardPreview() {
    MyWalletTheme {
        DashboardScreen(
            expenses = sampleExpenses,
            total = sampleExpenses.sumOf { it.amount },
            categoryTotals = sampleExpenses.groupBy { it.category }.mapValues { entry -> entry.value.sumOf { it.amount } },
            isEmpty = false,
            onDelete = {}
        )
    }
}
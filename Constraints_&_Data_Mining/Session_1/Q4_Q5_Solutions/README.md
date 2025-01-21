Pablo Mollá
Junjie Chen

# Exercise 5: Assignment

## Q1

### 1. Variables \(X\)
We create one variable for each cell in an \((n \times n)\) grid:

$$
X = \{ x^{i,j} \mid 1 \leq i \leq n,\ 1 \leq j \leq n\}
$$

Here, \(x^{i,j}\) corresponds to the cell in row \(i\) and column \(j\).  
There are \(n^2\) variables in total.

---

### 2. Domain \(D\)
Each variable \(x^{i,j}\) can take any value from \(\{1, 2, \ldots, n\}\):

$$
D(x^{i,j}) = \{1, 2, \ldots, n\}
$$

---

### 3. Constraints \(C\)

#### 3.1 Row Constraints
All variables in the same row \(i\) must take different values:

$$
\forall i \in \{1, \ldots, n\},\ \forall j \neq k,\ x^{i,j} \neq x^{i,k}
$$

---

#### 3.2 Column Constraints
All variables in the same column \(j\) must take different values:

$$
\forall j \in \{1, \ldots, n\},\ \forall i \neq k,\ x^{i,j} \neq x^{k,j}
$$

---

#### 3.3 Sub-grid (Block) Constraints
Since \(n = r^2\), we partition the board into $(r \times r)$ “blocks,” each of size $(r \times r)$. For each block (indexed by $(a,b)$ with $1 \leq a,b \leq r$, the cells in that block must all have different values. The block $(a,b)$ contains those cells $(i,j)$ such that:

$$
i \in \{(a-1)r + 1,\ \ldots,\ ar\}, \quad
j \in \{(b-1)r + 1,\ \ldots,\ br\}
$$

All variables in that block must take different values:

$$
\forall \, 1 \leq a,b \leq r,\ 
\forall (i,j) \neq (k,l) \text{ in the same block},\ 
x^{i,j} \neq x^{k,l}
$$

---

## Q2, Q3, Q4, and Q5
See the coding file for the remaining questions.

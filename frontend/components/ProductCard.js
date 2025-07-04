// ProductCard component
window.ProductCard = {
  render: function(product) {
    return `
      <div class="product-card" data-id="${product.id}">
        <img src="${product.imageURL || ''}" alt="${product.name}" />
        <div class="product-name">${product.name}</div>
        <div class="product-price">$${product.price.toFixed(2)}</div>
        <button class="add-to-cart-btn">Add to Cart</button>
      </div>
    `;
  }
}; 
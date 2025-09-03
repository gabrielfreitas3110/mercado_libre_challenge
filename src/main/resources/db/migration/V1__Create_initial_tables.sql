-- Create items table
CREATE TABLE items (
    sku VARCHAR(100) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create item_attributes table for storing item attributes as key-value pairs
CREATE TABLE item_attributes (
    sku VARCHAR(100) NOT NULL,
    attribute_key VARCHAR(100) NOT NULL,
    attribute_value TEXT,
    PRIMARY KEY (sku, attribute_key),
    FOREIGN KEY (sku) REFERENCES items(sku) ON DELETE CASCADE
);

-- Create inventory_records table
CREATE TABLE inventory_records (
    id BIGSERIAL PRIMARY KEY,
    sku VARCHAR(100) NOT NULL,
    store_id VARCHAR(100) NOT NULL,
    quantity_available BIGINT NOT NULL DEFAULT 0,
    reserved BIGINT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(sku, store_id)
);

-- Create reservations table
CREATE TABLE reservations (
    reservation_id VARCHAR(100) PRIMARY KEY,
    sku VARCHAR(100) NOT NULL,
    store_id VARCHAR(100) NOT NULL,
    qty BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED',
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better performance
CREATE INDEX idx_inventory_records_sku ON inventory_records(sku);
CREATE INDEX idx_inventory_records_store_id ON inventory_records(store_id);
CREATE INDEX idx_inventory_records_sku_store ON inventory_records(sku, store_id);

CREATE INDEX idx_reservations_sku ON reservations(sku);
CREATE INDEX idx_reservations_store_id ON reservations(store_id);
CREATE INDEX idx_reservations_status ON reservations(status);
CREATE INDEX idx_reservations_expires_at ON reservations(expires_at);
CREATE INDEX idx_reservations_sku_store ON reservations(sku, store_id);

-- Create function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers to automatically update updated_at
CREATE TRIGGER update_items_updated_at BEFORE UPDATE ON items
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_inventory_records_updated_at BEFORE UPDATE ON inventory_records
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Create data_items table
CREATE TABLE data_items (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    value TEXT,
    category VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better query performance
CREATE INDEX idx_data_items_category ON data_items(category);
CREATE INDEX idx_data_items_status ON data_items(status);
CREATE INDEX idx_data_items_created_at ON data_items(created_at);

-- Insert sample data
INSERT INTO data_items (name, description, value, category, status) VALUES
('Sample Data 1', 'This is a sample data item', 'value1', 'category1', 'ACTIVE'),
('Sample Data 2', 'Another sample data item', 'value2', 'category2', 'ACTIVE'),
('Sample Data 3', 'Third sample data item', 'value3', 'category1', 'ACTIVE');
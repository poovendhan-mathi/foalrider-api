-- =============================================
-- V1: Create Roles Table
-- =============================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    permissions JSONB DEFAULT '[]'::jsonb,
    is_system BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create index
CREATE INDEX idx_roles_name ON roles(name);

-- Insert default roles
INSERT INTO roles (name, description, permissions, is_system) VALUES
    ('ROLE_CUSTOMER', 'Regular customer with standard shopping permissions', 
     '["VIEW_PRODUCTS", "MANAGE_CART", "PLACE_ORDER", "VIEW_OWN_ORDERS", "MANAGE_PROFILE", "WRITE_REVIEWS"]'::jsonb, TRUE),
    
    ('ROLE_STAFF', 'Staff member with order management permissions', 
     '["VIEW_PRODUCTS", "MANAGE_CART", "PLACE_ORDER", "VIEW_OWN_ORDERS", "MANAGE_PROFILE", "WRITE_REVIEWS", "VIEW_ALL_ORDERS", "UPDATE_ORDER_STATUS", "VIEW_CUSTOMERS", "MANAGE_INVENTORY"]'::jsonb, TRUE),
    
    ('ROLE_ADMIN', 'Administrator with full product and order management', 
     '["VIEW_PRODUCTS", "MANAGE_CART", "PLACE_ORDER", "VIEW_OWN_ORDERS", "MANAGE_PROFILE", "WRITE_REVIEWS", "VIEW_ALL_ORDERS", "UPDATE_ORDER_STATUS", "VIEW_CUSTOMERS", "MANAGE_INVENTORY", "MANAGE_PRODUCTS", "MANAGE_CATEGORIES", "MANAGE_BRANDS", "MANAGE_COUPONS", "VIEW_ANALYTICS", "MANAGE_STAFF", "MODERATE_REVIEWS"]'::jsonb, TRUE),
    
    ('ROLE_SUPER_ADMIN', 'Super administrator with all permissions', 
     '["*"]'::jsonb, TRUE);

COMMENT ON TABLE roles IS 'User roles for RBAC';
COMMENT ON COLUMN roles.permissions IS 'JSON array of permission strings';

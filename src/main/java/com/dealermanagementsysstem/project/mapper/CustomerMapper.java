package com.dealermanagementsysstem.project.mapper;

import com.dealermanagementsysstem.project.Model.DTOCustomer;
import com.dealermanagementsysstem.project.dto.CustomerForm;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface CustomerMapper {
    CustomerMapper INSTANCE = Mappers.getMapper(CustomerMapper.class);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    DTOCustomer toEntity(CustomerForm customerForm);

    CustomerForm toCustomerForm(DTOCustomer dtoCustomer);


    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateCustomer(@MappingTarget DTOCustomer dtoCustomer, CustomerForm customerForm);
}

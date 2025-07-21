/**
 * 수정된 스마트 이미지 시스템 (Commerce Service 직접 접근)
 */
export function useSmartImages() {
    const BASE_IMAGE_PATH = '/images/banners/products/'
    // 🔥 Commerce Service에 직접 접근 (8090 포트)
    const COMMERCE_SERVICE_URL = 'http://13.209.253.241:8090'

    const getProductImage = (product) => {
        console.log('🔍 getProductImage 호출됨:', product)

        // 🔥 관리자가 업로드한 이미지 - Commerce Service 직접 접근
        if (product.mainImage && product.mainImage.startsWith('/upload/')) {
            const fileName = product.mainImage.split('/').pop()
            const finalUrl = `${COMMERCE_SERVICE_URL}/images/${fileName}`
            console.log('✅ 업로드 이미지 (mainImage):', finalUrl)
            return finalUrl
        }

        // 🔥 image 필드도 업로드된 파일인 경우
        if (product.image && product.image.startsWith('/upload/')) {
            const fileName = product.image.split('/').pop()
            const finalUrl = `${COMMERCE_SERVICE_URL}/images/${fileName}`
            console.log('✅ 업로드 이미지 (image):', finalUrl)
            return finalUrl
        }

        // 🔥 파일명만 있는 경우 (관리자 업로드)
        if (product.mainImage && !product.mainImage.startsWith('/') && !product.mainImage.startsWith('http')) {
            const finalUrl = `${COMMERCE_SERVICE_URL}/images/${product.mainImage}`
            console.log('✅ 파일명 이미지 (mainImage):', finalUrl)
            return finalUrl
        }

        // 🔥 image 필드가 파일명만 있는 경우
        if (product.image && !product.image.startsWith('/') && !product.image.startsWith('http')) {
            const finalUrl = `${COMMERCE_SERVICE_URL}/images/${product.image}`
            console.log('✅ 파일명 이미지 (image):', finalUrl)
            return finalUrl
        }

        // 이미 완전한 URL인 경우 (http로 시작하는 외부 이미지)
        if (product.image && product.image.startsWith('http')) {
            console.log('✅ 외부 이미지:', product.image)
            return product.image
        }

        // 이미 완전한 경로인 경우 (/images로 시작) - 기본 이미지들
        if (product.image && product.image.startsWith('/images')) {
            console.log('✅ 기본 이미지:', product.image)
            return product.image
        }

        // 기본 이미지
        const defaultImage = '/images/banners/products/default-product.jpg'
        console.log('⚠️ 기본 이미지 사용:', defaultImage)
        return defaultImage
    }

    const handleImageError = (event) => {
        console.log('❌ 이미지 로드 실패:', event.target.src)

        // 첫 번째 실패: 기본 이미지로 교체
        if (!event.target.src.includes('default-product.jpg')) {
            console.log('🔄 기본 이미지로 교체')
            event.target.src = '/images/banners/products/default-product.jpg'
        } else {
            // 기본 이미지도 실패하면 숨김 처리
            console.log('🚫 기본 이미지도 실패, 숨김 처리')
            event.target.style.display = 'none'
        }
    }

    const handleImageLoad = (event) => {
        console.log('✅ 이미지 로드 성공:', event.target.src)
        event.target.style.opacity = '1'
    }

    return {
        getProductImage,
        handleImageError,
        handleImageLoad
    }
}